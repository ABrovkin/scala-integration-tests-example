package com.abrovkin.config

import pureconfig.ConfigReader

case class ExternalServiceConfig(externalServiceUri: String) derives ConfigReader
