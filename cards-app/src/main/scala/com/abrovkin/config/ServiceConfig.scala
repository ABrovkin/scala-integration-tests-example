package com.abrovkin.config

import com.abrovkin.cache.RedisConfig
import com.abrovkin.external.ExternalServiceConfig
import pureconfig.ConfigReader

case class ServiceConfig(
    httpHost: String,
    httpPort: Int,
    redisConfig: RedisConfig,
    externalServiceConfig: ExternalServiceConfig
) derives ConfigReader
