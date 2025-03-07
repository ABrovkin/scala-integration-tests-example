package com.abrovkin.config

import pureconfig.ConfigReader

case class RedisConfig(redisHost: String, redisPort: Int, timeout: Int) derives ConfigReader
