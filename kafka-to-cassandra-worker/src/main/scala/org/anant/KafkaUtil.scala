package org.anant

import org.apache.kafka.common.serialization.StringDeserializer
import java.util.Properties

import scala.io.Source

object KafkaUtil {

  def getProperties(projectProps : Properties, debugMode : Boolean): Properties = {
    /* 
     * sets properties for kafka
     */
    val props = new Properties()

    // set default initialOffset based on debugMode
		var initialOffset = if (debugMode) "earliest" else "latest"
    // override if set in the properties
    if (projectProps.getProperty("kafka.initial-offset", null) != null) {
      initialOffset = projectProps.getProperty("kafka.initial-offset") 
    }

    props.setProperty("bootstrap.servers", projectProps.getProperty("kafka.host"))
    // NOTE not currently using keys
    props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer") 
    props.setProperty("group.id", projectProps.getProperty("kafka.consumer.group"))
    props.setProperty("auto.offset.reset", initialOffset)
    props.setProperty("enable.auto.commit", "false")

    // if we are using a schema, set config for schema
    if (projectProps.getProperty("kafka.schema") == "true") {
      props.setProperty("schema.registry.url", projectProps.getProperty("kafka.schema.registry.url"))
      props.setProperty("value.deserializer", "io.confluent.kafka.serializers.KafkaAvroDeserializer")

    } else {
      props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    }

    // if using kstreams, will need to set some other configs. Default is false
    if (projectProps.getProperty("streaming", "false") == "true") {
      // "Each stream processing application must have a unique ID. The same ID must be given to all instances of the application"
      props.setProperty("application.id", "streaming-leaves-with-avro-schema") 
    }


    if (debugMode) {
      println(s"Using Debug mode, so auto.offset.reset is ${props.getProperty("auto.offset.reset")}")
    }

    props
  }

}
