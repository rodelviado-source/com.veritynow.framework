package util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class JSON {
	
	public static final ObjectMapper MAPPER = JsonMapper.builder()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(SerializationFeature.INDENT_OUTPUT, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            //.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
            //.configure(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES, false)
            .build();
	public static final ObjectMapper MAPPER_PRETTY = JsonMapper.builder()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false)
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            //.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
            //.configure(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES, false)
            .build();
}
