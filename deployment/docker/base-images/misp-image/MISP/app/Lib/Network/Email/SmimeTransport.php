<?php
/**
 * Send mail using mail() function
 * with SMIME headers
 */

/**
 * Send mail using mail() function
 *
 * @package       Cake.Network.Email
 */
class SmimeTransport extends AbstractTransport {

	/**
	 * Send mail
	 *
	 * @param CakeEmail $email CakeEmail
	 * @return array
	 * @throws SocketException When mail cannot be sent.
	 */
	public function send(CakeEmail $email) {
		$eol = PHP_EOL;
		if (isset($this->_config['eol'])) {
			$eol = $this->_config['eol'];
		}
		$headers = $email->getHeaders(array('from', 'sender', 'replyTo', 'readReceipt', 'returnPath', 'to', 'cc', 'bcc'));
		// DIRTY HEADERS
		$headers = array_merge($headers, array('Content-Transfer-Encoding' => 'base64', 'Content-Type' => 'application/pkcs7-mime; name=smime.p7m; smime-type=enveloped-data', 'Content-Disposition' => 'attachment; filename=smime.p7m', 'Content-Description' => 'S/MIME Encrypted Message'));
		$to = $headers['To'];
		unset($headers['To']);
		foreach ($headers as $key => $header) {
			$headers[$key] = str_replace(array("\r", "\n"), '', $header);
		}
		$headers = $this->_headersToString($headers, $eol);
		$subject = str_replace(array("\r", "\n"), '', $email->subject());
		$to = str_replace(array("\r", "\n"), '', $to);

		$message = implode($eol, $email->message());

		$params = isset($this->_config['additionalParameters']) ? $this->_config['additionalParameters'] : null;
		$this->_mail($to, $subject, $message, $headers, $params);
		return array('headers' => $headers, 'message' => $message);
	}

	/**
	 * Wraps internal function mail() and throws exception instead of errors if anything goes wrong
	 *
	 * @param string $to email's recipient
	 * @param string $subject email's subject
	 * @param string $message email's body
	 * @param string $headers email's custom headers
	 * @param string $params additional params for sending email, will be ignored when in safe_mode
	 * @throws SocketException if mail could not be sent
	 * @return void
	 */
	protected function _mail($to, $subject, $message, $headers, $params = null) {
		if (ini_get('safe_mode')) {
			//@codingStandardsIgnoreStart
			if (!@mail($to, $subject, $message, $headers)) {
				$error = error_get_last();
				$msg = 'Could not send email: ' . isset($error['message']) ? $error['message'] : 'unknown';
				throw new SocketException($msg);
			}
		} else if (!@mail($to, $subject, $message, $headers, $params)) {
			$error = error_get_last();
			$msg = 'Could not send email: ' . isset($error['message']) ? $error['message'] : 'unknown';
			//@codingStandardsIgnoreEnd
			throw new SocketException($msg);
		}
	}

}
