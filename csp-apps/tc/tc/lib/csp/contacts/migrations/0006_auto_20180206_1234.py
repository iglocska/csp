# -*- coding: utf-8 -*-
# Generated by Django 1.11.9 on 2018-02-06 12:34
from __future__ import unicode_literals

import django.contrib.postgres.fields
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('contacts', '0005_auto_20180205_1423'),
    ]

    operations = [
        migrations.AlterField(
            model_name='incomingteamcontact',
            name='created',
            field=models.DateTimeField(auto_now_add=True, verbose_name='Received on'),
        ),
        migrations.AlterField(
            model_name='incomingteamcontact',
            name='csp_id',
            field=models.TextField(verbose_name='Sent from'),
        ),
        migrations.AlterField(
            model_name='incomingteamcontact',
            name='target_circle_id',
            field=django.contrib.postgres.fields.ArrayField(base_field=models.TextField(), blank=True, size=None, verbose_name='Shared with CTC'),
        ),
        migrations.AlterField(
            model_name='incomingteamcontact',
            name='target_team_id',
            field=django.contrib.postgres.fields.ArrayField(base_field=models.TextField(), blank=True, size=None, verbose_name='Shared with Team'),
        ),
    ]
