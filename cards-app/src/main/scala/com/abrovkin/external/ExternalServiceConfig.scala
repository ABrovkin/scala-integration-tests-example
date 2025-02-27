package com.abrovkin.external

import pureconfig.ConfigReader

case class ExternalServiceConfig(externalServiceUri: String) derives ConfigReader
