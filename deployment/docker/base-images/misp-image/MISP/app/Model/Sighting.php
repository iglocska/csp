<?php
App::uses('AppModel', 'Model');
App::uses('RandomTool', 'Tools');

class Sighting extends AppModel {

	public $useTable = 'sightings';

	public $recursive = -1;

	public $actsAs = array(
			'Containable',
	);

	public $validate = array(
		'event_id' => 'numeric',
		'attribute_id' => 'numeric',
		'org_id' => 'numeric',
		'date_sighting' => 'numeric',
		'type' => array(
			'rule' => array('inList', array(0, 1, 2)),
			'message' => 'Invalid type. Valid options are: 0 (Sighting), 1 (False-positive), 2 (Expiration).'
		)
	);

	public $belongsTo = array(
			'Attribute',
			'Event',
			'Organisation' => array(
					'className' => 'Organisation',
					'foreignKey' => 'org_id'
			),
	);

	public $type = array(
		0 => 'sighting',
		1 => 'false-positive',
		2 => 'expiration'
	);

	public function beforeValidate($options = array()) {
		parent::beforeValidate();
		$date = date('Y-m-d H:i:s');
		if (empty($this->data['Sighting']['id']) && empty($this->data['Sighting']['date_sighting'])) {
			$this->data['Sighting']['date_sighting'] = $date;
		}
		if (empty($this->data['Sighting']['uuid'])) {
			$this->data['Sighting']['uuid'] = CakeText::uuid();
		}
		return true;
	}

	public function afterSave($created, $options = array()) {
		if (Configure::read('Plugin.ZeroMQ_enable') && Configure::read('Plugin.ZeroMQ_sighting_notifications_enable')) {
			$pubSubTool = $this->getPubSubTool();
			$pubSubTool->sighting_save($this->data);
		}
		return true;
	}

	public function attachToEvent($event, $user, $attribute_id = false, $extraConditions = false) {
		$ownEvent = false;
		if ($user['Role']['perm_site_admin'] || $event['Event']['org_id'] == $user['org_id']) $ownEvent = true;
		$conditions = array('Sighting.event_id' => $event['Event']['id']);
		if ($attribute_id) {
			$conditions[] = array('Sighting.attribute_id' => $attribute_id);
		}
		if (!$ownEvent && (!Configure::read('Plugin.Sightings_policy') || Configure::read('Plugin.Sightings_policy') == 0)) {
			$conditions['Sighting.org_id'] = $user['org_id'];
		}
		if ($extraConditions !== false) {
			$conditions['AND'] = $extraConditions;
		}
		$contain = array();
		if (Configure::read('MISP.showorg')) {
			$contain['Organisation'] = array('fields' => array('Organisation.id', 'Organisation.uuid', 'Organisation.name'));
		}

		// Sighting reporters setting
		// If the event has any sightings for the user's org, then the user is a sighting reporter for the event too.
		// This means that he /she has access to the sightings data contained within
		if (!$ownEvent && Configure::read('Plugin.Sightings_policy') == 1) {
			$temp = $this->find('first', array('recursive' => -1, 'conditions' => array('Sighting.event_id' => $event['Event']['id'], 'Sighting.org_id' => $user['org_id'])));
			if (empty($temp)) return array();
		}

		$sightings = $this->find('all', array(
				'conditions' => $conditions,
				'recursive' => -1,
				'contain' => $contain,
		));
		if (empty($sightings)) return array();
		$anonymise = Configure::read('Plugin.Sightings_anonymise');

		foreach ($sightings as $k => $sighting) {
			if ($anonymise && !$user['Role']['perm_site_admin']) {
				if ($sighting['Sighting']['org_id'] != $user['org_id']) {
					unset($sightings[$k]['Sighting']['org_id']);
					unset($sightings[$k]['Organisation']);
				}
			}
			// rearrange it to match the event format of fetchevent
			if (isset($sightings[$k]['Organisation'])) {
				$sightings[$k]['Sighting']['Organisation'] = $sightings[$k]['Organisation'];
			}
			$sightings[$k] = $sightings[$k]['Sighting'] ;
		}
		return $sightings;
	}

	public function saveSightings($id, $values, $timestamp, $user, $type = false, $source = false) {
		$conditions = array();
		if ($id && $id !== 'stix') {
			$id = $this->explodeIdList($id);
			if (!is_array($id) && strlen($id) == 36) $conditions = array('Attribute.uuid' => $id);
			else $conditions = array('Attribute.id' => $id);
		} else {
			if (!$values) return 'No valid attributes found.';
			foreach ($values as $value) {
				foreach (array('value1', 'value2') as $field) {
					$conditions['OR'][] = array(
						'LOWER(Attribute.' . $field . ') LIKE' => strtolower($value)
					);
				}
			}
		}
		if (!in_array($type, array(0, 1, 2))) {
			return 'Invalid type, please change it before you POST 1000000 sightings.';
		}
		$attributes = $this->Attribute->fetchAttributes($user, array('conditions' => $conditions));
		if (empty($attributes)) return 'No valid attributes found that match the criteria.';
		$sightingsAdded = 0;
		foreach ($attributes as $attribute) {
			if ($type === '2') {
				// remove existing expiration by the same org if it exists
				$this->deleteAll(array('Sighting.org_id' => $user['org_id'], 'Sighting.type' => $type, 'Sighting.attribute_id' => $attribute['Attribute']['id']));
			}
			$this->create();
			$sighting = array(
					'attribute_id' => $attribute['Attribute']['id'],
					'event_id' => $attribute['Attribute']['event_id'],
					'org_id' => $user['org_id'],
					'date_sighting' => $timestamp,
					'type' => $type,
					'source' => $source
			);
			$result = $this->save($sighting);
			if ($result === false) {
				return json_encode($this->validationErrors);
			}
			$sightingsAdded += $result ? 1 : 0;
		}
		if ($sightingsAdded == 0) {
			return 'There was nothing to add.';
		}
		return $sightingsAdded;
	}

	public function handleStixSighting($data) {
		$randomFileName = $this->generateRandomFileName();
		$tempFile = new File(APP . "files" . DS . "scripts" . DS . "tmp" . DS . $randomFileName, true, 0644);

		// save the json_encoded event(s) to the temporary file
		if (!$tempFile->write($data)) return array('success' => 0, 'message' => 'Could not write the Sightings file to disk.');
		$tempFile->close();
		$scriptFile = APP . "files" . DS . "scripts" . DS . "stixsighting2misp.py";
		// Execute the python script and point it to the temporary filename
		$result = shell_exec('python ' . $scriptFile . ' ' . $randomFileName);
		// The result of the script will be a returned JSON object with 2 variables: success (boolean) and message
		// If success = 1 then the temporary output file was successfully written, otherwise an error message is passed along
		$result = json_decode($result, true);

		if ($result['success'] == 1) {
			$file = new File(APP . "files" . DS . "scripts" . DS . "tmp" . DS . $randomFileName . ".out");
			$result['data'] = $file->read();
			$file->close();
			$file->delete();
		}
		$tempFile->delete();
		return $result;
	}

	public function generateRandomFileName() {
		return (new RandomTool())->random_str(FALSE, 12);
	}

	public function addUuids() {
		$sightings = $this->find('all', array(
			'recursive' => -1,
			'conditions' => array('uuid' => '')
		));
		$this->saveMany($sightings);
		return true;
	}

	public function explodeIdList($id) {
		if (strpos($id, '|')) {
			$id = explode('|', $id);
			foreach ($id as $k => $v) {
				if (!is_numeric($v)) {
					unset($id[$k]);
				}
			}
			$id = array_values($id);
		}
		return $id;
	}

	public function getSightingsForObjectIds($user, $tagList, $context = 'event', $type = '0') {
		$range = (!empty(Configure::read('MISP.Sightings_range')) && is_numeric(Configure::read('MISP.Sightings_range'))) ? Configure::read('MISP.Sightings_range') : 365;
		$conditions = array(
			'Sighting.date_sighting >' => strtotime("-" . $range . " days"),
			ucfirst($context) . 'Tag.tag_id' => $tagList

		);
		$contain = array(
			ucfirst($context) => array(
				ucfirst($context) . 'Tag' => array(
					'Tag'
				)
			)
		);
		if ($type !== false) {
			$conditions['Sighting.type'] = $type;
		}
		$this->bindModel(array('hasOne' => array(ucfirst($context) . 'Tag' => array('foreignKey' => false, 'conditions' => ucfirst($context) . 'Tag.' . $context . '_id = Sighting.' . $context . '_id'))));
		$sightings = $this->find('all', array(
			'recursive' => -1,
			'contain' => array(ucfirst($context) . 'Tag'),
			'conditions' => $conditions,
			'fields' => array('Sighting.id', 'Sighting.' . $context . '_id', 'Sighting.date_sighting', ucfirst($context) . 'Tag.tag_id')
		));
		$sightingsRearranged = array();
		foreach ($sightings as $sighting) {
			$date = date("Y-m-d", $sighting['Sighting']['date_sighting']);
			if (isset($sightingsRearranged[$sighting['Sighting'][$context . '_id']][$date])) {
				$sightingsRearranged[$sighting['Sighting'][$context . '_id']][$date]++;
			} else {
				$sightingsRearranged[$sighting['Sighting'][$context . '_id']][$date] = 1;
			}
		}
		return $sightingsRearranged;
	}
}
