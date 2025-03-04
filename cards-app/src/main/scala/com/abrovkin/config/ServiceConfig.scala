package com.abrovkin.config

import pureconfig.ConfigReader

case class ServiceConfig(
    httpHost: String,
    httpPort: Int,
    redisConfig: RedisConfig,
    externalServiceConfig: ExternalServiceConfig
) derives ConfigReader
