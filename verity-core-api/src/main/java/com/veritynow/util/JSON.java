package com.veritynow.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonInclude.Value;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.EnumNamingStrategy;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.CacheProvider;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.cfg.DatatypeFeature;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MutableCoercionConfig;
import com.fasterxml.jackson.databind.cfg.MutableConfigOverride;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.introspect.AccessorNamingStrategy.Provider;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector.MixInResolver;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class JSON {
	
	
	
	private static final ObjectMapper MAPPER = JsonMapper.builder()
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
	
	
	
	
	public static ObjectMapper copy() {
		return MAPPER.copy();
	}
	
	public static ObjectMapper copyWith(JsonFactory factory) {
		return MAPPER.copyWith(factory);
	}
	
	public static Version version() {
		return MAPPER.version();
	}
	
	public static ObjectMapper registerModule(Module module) {
		return MAPPER.registerModule(module);
	}
	
	public static ObjectMapper registerModules(Module... modules) {
		return MAPPER.registerModules(modules);
	}
	
	public static ObjectMapper registerModules(Iterable<? extends Module> modules) {
		return MAPPER.registerModules(modules);
	}
	
	public static Set<Object> getRegisteredModuleIds() {
		return MAPPER.getRegisteredModuleIds();
	}
	
	public static ObjectMapper findAndRegisterModules() {
		return MAPPER.findAndRegisterModules();
	}
	
	public static JsonGenerator createGenerator(OutputStream out) throws IOException {
		return MAPPER.createGenerator(out);
	}
	
	public static JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
		return MAPPER.createGenerator(out, enc);
	}
	
	public static JsonGenerator createGenerator(Writer w) throws IOException {
		return MAPPER.createGenerator(w);
	}
	
	public static JsonGenerator createGenerator(File outputFile, JsonEncoding enc) throws IOException {
		return MAPPER.createGenerator(outputFile, enc);
	}
	
	public static JsonGenerator createGenerator(DataOutput out) throws IOException {
		return MAPPER.createGenerator(out);
	}
	
	public static JsonParser createParser(File src) throws IOException {
		return MAPPER.createParser(src);
	}
	
	public static JsonParser createParser(InputStream in) throws IOException {
		return MAPPER.createParser(in);
	}
	
	public static JsonParser createParser(Reader r) throws IOException {
		return MAPPER.createParser(r);
	}
	
	public static JsonParser createParser(byte[] content) throws IOException {
		return MAPPER.createParser(content);
	}
	
	public static JsonParser createParser(byte[] content, int offset, int len) throws IOException {
		return MAPPER.createParser(content, offset, len);
	}
	
	public static JsonParser createParser(String content) throws IOException {
		return MAPPER.createParser(content);
	}
	
	public static JsonParser createParser(char[] content) throws IOException {
		return MAPPER.createParser(content);
	}
	
	public static JsonParser createParser(char[] content, int offset, int len) throws IOException {
		return MAPPER.createParser(content, offset, len);
	}
	
	public static JsonParser createParser(DataInput content) throws IOException {
		return MAPPER.createParser(content);
	}
	
	public static JsonParser createNonBlockingByteArrayParser() throws IOException {
		return MAPPER.createNonBlockingByteArrayParser();
	}
	
	public static SerializationConfig getSerializationConfig() {
		return MAPPER.getSerializationConfig();
	}
	
	public static DeserializationConfig getDeserializationConfig() {
		return MAPPER.getDeserializationConfig();
	}
	
	public static DeserializationContext getDeserializationContext() {
		return MAPPER.getDeserializationContext();
	}
	
	public static ObjectMapper setSerializerFactory(SerializerFactory f) {
		
		return MAPPER.setSerializerFactory(f);
	}
	
	public static SerializerFactory getSerializerFactory() {
		
		return MAPPER.getSerializerFactory();
	}
	
	public static ObjectMapper setSerializerProvider(DefaultSerializerProvider p) {
		
		return MAPPER.setSerializerProvider(p);
	}
	
	public static SerializerProvider getSerializerProvider() {
		
		return MAPPER.getSerializerProvider();
	}
	
	public static SerializerProvider getSerializerProviderInstance() {
		
		return MAPPER.getSerializerProviderInstance();
	}
	
	public static ObjectMapper setMixIns(Map<Class<?>, Class<?>> sourceMixins) {
		
		return MAPPER.setMixIns(sourceMixins);
	}
	
	public static ObjectMapper addMixIn(Class<?> target, Class<?> mixinSource) {
		
		return MAPPER.addMixIn(target, mixinSource);
	}
	
	public static ObjectMapper setMixInResolver(MixInResolver resolver) {
		
		return MAPPER.setMixInResolver(resolver);
	}
	
	public static Class<?> findMixInClassFor(Class<?> cls) {
		
		return MAPPER.findMixInClassFor(cls);
	}
	
	public static int mixInCount() {
		
		return MAPPER.mixInCount();
	}
	
	public static VisibilityChecker<?> getVisibilityChecker() {
		
		return MAPPER.getVisibilityChecker();
	}
	
	public static ObjectMapper setVisibility(VisibilityChecker<?> vc) {
		
		return MAPPER.setVisibility(vc);
	}
	
	public static ObjectMapper setVisibility(PropertyAccessor forMethod, Visibility visibility) {
		
		return MAPPER.setVisibility(forMethod, visibility);
	}
	
	public static SubtypeResolver getSubtypeResolver() {
		
		return MAPPER.getSubtypeResolver();
	}
	
	public static ObjectMapper setSubtypeResolver(SubtypeResolver str) {
		
		return MAPPER.setSubtypeResolver(str);
	}
	
	public static ObjectMapper setAnnotationIntrospector(AnnotationIntrospector ai) {
		
		return MAPPER.setAnnotationIntrospector(ai);
	}
	
	public static ObjectMapper setAnnotationIntrospectors(AnnotationIntrospector serializerAI,
			AnnotationIntrospector deserializerAI) {
		return MAPPER.setAnnotationIntrospectors(serializerAI, deserializerAI);
	}
	
	public static ObjectMapper setPropertyNamingStrategy(PropertyNamingStrategy s) {
		return MAPPER.setPropertyNamingStrategy(s);
	}
	
	public static PropertyNamingStrategy getPropertyNamingStrategy() {
		return MAPPER.getPropertyNamingStrategy();
	}
	
	public static ObjectMapper setEnumNamingStrategy(EnumNamingStrategy s) {
		return MAPPER.setEnumNamingStrategy(s);
	}
	
	public static EnumNamingStrategy getEnumNamingStrategy() {
		return MAPPER.getEnumNamingStrategy();
	}
	
	public static ObjectMapper setAccessorNaming(Provider s) {
		return MAPPER.setAccessorNaming(s);
	}
	
	public static ObjectMapper setDefaultPrettyPrinter(PrettyPrinter pp) {
		return MAPPER.setDefaultPrettyPrinter(pp);
	}
	
	public static ObjectMapper setPolymorphicTypeValidator(PolymorphicTypeValidator ptv) {
		return MAPPER.setPolymorphicTypeValidator(ptv);
	}
	
	public static PolymorphicTypeValidator getPolymorphicTypeValidator() {
		return MAPPER.getPolymorphicTypeValidator();
	}
	
	public static ObjectMapper setDefaultPropertyInclusion(Value incl) {
		return MAPPER.setDefaultPropertyInclusion(incl);
	}
	
	public static ObjectMapper setDefaultPropertyInclusion(Include incl) {
		return MAPPER.setDefaultPropertyInclusion(incl);
	}
	
	public static ObjectMapper setDefaultSetterInfo(com.fasterxml.jackson.annotation.JsonSetter.Value v) {
		return MAPPER.setDefaultSetterInfo(v);
	}
	
	public static ObjectMapper setDefaultVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Value vis) {
		return MAPPER.setDefaultVisibility(vis);
	}
	
	public static ObjectMapper setDefaultMergeable(Boolean b) {
		return MAPPER.setDefaultMergeable(b);
	}
	
	public static ObjectMapper setDefaultLeniency(Boolean b) {
		return MAPPER.setDefaultLeniency(b);
	}
	
	public static void registerSubtypes(Class<?>... classes) {
		
		MAPPER.registerSubtypes(classes);
	}
	
	public static void registerSubtypes(NamedType... types) {
		
		MAPPER.registerSubtypes(types);
	}
	
	public static void registerSubtypes(Collection<Class<?>> subtypes) {
		
		MAPPER.registerSubtypes(subtypes);
	}
	
	public static ObjectMapper activateDefaultTyping(PolymorphicTypeValidator ptv) {
		return MAPPER.activateDefaultTyping(ptv);
	}
	
	public static ObjectMapper activateDefaultTyping(PolymorphicTypeValidator ptv, DefaultTyping applicability) {
		return  MAPPER.activateDefaultTyping(ptv, applicability);
	}
	public ObjectMapper activateDefaultTyping(PolymorphicTypeValidator ptv, DefaultTyping applicability, As includeAs) {
		
		return MAPPER.activateDefaultTyping(ptv, applicability, includeAs);
	}
	
	public static ObjectMapper activateDefaultTypingAsProperty(PolymorphicTypeValidator ptv, DefaultTyping applicability,
			String propertyName) {
		
		return MAPPER.activateDefaultTypingAsProperty(ptv, applicability, propertyName);
	}
	
	public static ObjectMapper deactivateDefaultTyping() {
		
		return MAPPER.deactivateDefaultTyping();
	}
	
	public static ObjectMapper setDefaultTyping(TypeResolverBuilder<?> typer) {
		
		return MAPPER.setDefaultTyping(typer);
	}
	
	public static MutableConfigOverride configOverride(Class<?> type) {
		
		return MAPPER.configOverride(type);
	}
	
	public static MutableCoercionConfig coercionConfigDefaults() {
		
		return MAPPER.coercionConfigDefaults();
	}
	
	public static MutableCoercionConfig coercionConfigFor(LogicalType logicalType) {
		
		return MAPPER.coercionConfigFor(logicalType);
	}
	
	public static MutableCoercionConfig coercionConfigFor(Class<?> physicalType) {
		
		return MAPPER.coercionConfigFor(physicalType);
	}
	
	public static TypeFactory getTypeFactory() {
		
		return MAPPER.getTypeFactory();
	}
	
	public static ObjectMapper setTypeFactory(TypeFactory f) {
		
		return MAPPER.setTypeFactory(f);
	}
	
	public static JavaType constructType(Type t) {
		
		return MAPPER.constructType(t);
	}
	
	public static JavaType constructType(TypeReference<?> typeRef) {
		
		return MAPPER.constructType(typeRef);
	}
	
	public static JsonNodeFactory getNodeFactory() {
		
		return MAPPER.getNodeFactory();
	}
	
	public static ObjectMapper setNodeFactory(JsonNodeFactory f) {
		
		return MAPPER.setNodeFactory(f);
	}
	
	public static ObjectMapper setConstructorDetector(ConstructorDetector cd) {
		
		return MAPPER.setConstructorDetector(cd);
	}
	
	public static ObjectMapper setCacheProvider(CacheProvider cacheProvider) {
		
		return MAPPER.setCacheProvider(cacheProvider);
	}
	
	public static ObjectMapper addHandler(DeserializationProblemHandler h) {
		
		return MAPPER.addHandler(h);
	}
	
	public static ObjectMapper clearProblemHandlers() {
		
		return MAPPER.clearProblemHandlers();
	}
	
	public static ObjectMapper setConfig(DeserializationConfig config) {
		
		return MAPPER.setConfig(config);
	}

	
	public static ObjectMapper setFilterProvider(FilterProvider filterProvider) {
		
		return MAPPER.setFilterProvider(filterProvider);
	}
	
	public static ObjectMapper setBase64Variant(Base64Variant v) {
		
		return MAPPER.setBase64Variant(v);
	}
	
	public static ObjectMapper setConfig(SerializationConfig config) {
		
		return MAPPER.setConfig(config);
	}
	
	public static JsonFactory tokenStreamFactory() {
		
		return MAPPER.tokenStreamFactory();
	}
	
	public static JsonFactory getFactory() {
		
		return MAPPER.getFactory();
	}
	
	public static ObjectMapper setDateFormat(DateFormat dateFormat) {
		
		return MAPPER.setDateFormat(dateFormat);
	}
	
	public static DateFormat getDateFormat() {
		
		return MAPPER.getDateFormat();
	}
	
	public static Object setHandlerInstantiator(HandlerInstantiator hi) {
		
		return MAPPER.setHandlerInstantiator(hi);
	}
	
	public static ObjectMapper setInjectableValues(InjectableValues injectableValues) {
		
		return MAPPER.setInjectableValues(injectableValues);
	}
	
	public static InjectableValues getInjectableValues() {
		
		return MAPPER.getInjectableValues();
	}
	
	public static ObjectMapper setLocale(Locale l) {
		
		return MAPPER.setLocale(l);
	}
	
	public static ObjectMapper setTimeZone(TimeZone tz) {
		
		return MAPPER.setTimeZone(tz);
	}
	
	public static ObjectMapper setDefaultAttributes(ContextAttributes attrs) {
		
		return MAPPER.setDefaultAttributes(attrs);
	}
	
	public static boolean isEnabled(MapperFeature f) {
		
		return MAPPER.isEnabled(f);
	}
	
	public static boolean isEnabled(SerializationFeature f) {
		
		return MAPPER.isEnabled(f);
	}
	
	public static ObjectMapper configure(SerializationFeature f, boolean state) {
		
		return MAPPER.configure(f, state);
	}
	
	public static ObjectMapper enable(SerializationFeature f) {
		
		return MAPPER.enable(f);
	}
	
	public static ObjectMapper enable(SerializationFeature first, SerializationFeature... f) {
		
		return MAPPER.enable(first, f);
	}
	
	public static ObjectMapper disable(SerializationFeature f) {
		
		return MAPPER.disable(f);
	}
	
	public static ObjectMapper disable(SerializationFeature first, SerializationFeature... f) {
		
		return MAPPER.disable(first, f);
	}
	
	public static boolean isEnabled(DeserializationFeature f) {
		
		return MAPPER.isEnabled(f);
	}
	
	public static ObjectMapper configure(DeserializationFeature f, boolean state) {
		
		return MAPPER.configure(f, state);
	}
	
	public static ObjectMapper enable(DeserializationFeature feature) {
		
		return MAPPER.enable(feature);
	}
	
	public static ObjectMapper enable(DeserializationFeature first, DeserializationFeature... f) {
		
		return MAPPER.enable(first, f);
	}
	
	public static ObjectMapper disable(DeserializationFeature feature) {
		
		return MAPPER.disable(feature);
	}
	
	public static ObjectMapper disable(DeserializationFeature first, DeserializationFeature... f) {
		
		return MAPPER.disable(first, f);
	}
	
	public static ObjectMapper configure(DatatypeFeature f, boolean state) {
		
		return MAPPER.configure(f, state);
	}
	
	public static ObjectMapper configure(Feature f, boolean state) {
		
		return MAPPER.configure(f, state);
	}
	
	public static ObjectMapper configure(com.fasterxml.jackson.core.JsonGenerator.Feature f, boolean state) {
		
		return MAPPER.configure(f, state);
	}
	
	public static ObjectMapper enable(Feature... features) {
		
		return MAPPER.enable(features);
	}
	
	public static ObjectMapper enable(com.fasterxml.jackson.core.JsonGenerator.Feature... features) {
		
		return MAPPER.enable(features);
	}
	
	public static ObjectMapper disable(Feature... features) {
		
		return MAPPER.disable(features);
	}
	
	public static ObjectMapper disable(com.fasterxml.jackson.core.JsonGenerator.Feature... features) {
		
		return MAPPER.disable(features);
	}
	
	public static boolean isEnabled(Feature f) {
		
		return MAPPER.isEnabled(f);
	}
	
	public static boolean isEnabled(com.fasterxml.jackson.core.JsonGenerator.Feature f) {
		
		return MAPPER.isEnabled(f);
	}
	
	public static boolean isEnabled(com.fasterxml.jackson.core.JsonFactory.Feature f) {
		
		return MAPPER.isEnabled(f);
	}
	
	public static boolean isEnabled(StreamReadFeature f) {
		
		return MAPPER.isEnabled(f);
	}
	
	public static boolean isEnabled(StreamWriteFeature f) {
		
		return MAPPER.isEnabled(f);
	}
	
	public static <T> T readValue(JsonParser p, Class<T> valueType)
			throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(p, valueType);
	}
	
	public static <T> T readValue(JsonParser p, TypeReference<T> valueTypeRef)
			throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(p, valueTypeRef);
	}
	
	public static <T> T readValue(JsonParser p, JavaType valueType)
			throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(p, valueType);
	}
	
	public static <T extends TreeNode> T readTree(JsonParser p) throws IOException {
		
		return MAPPER.readTree(p);
	}
	
	public static <T> MappingIterator<T> readValues(JsonParser p, ResolvedType valueType) throws IOException {
		
		return MAPPER.readValues(p, valueType);
	}
	
	public static <T> MappingIterator<T> readValues(JsonParser p, JavaType valueType) throws IOException {
		
		return MAPPER.readValues(p, valueType);
	}
	
	public static <T> MappingIterator<T> readValues(JsonParser p, Class<T> valueType) throws IOException {
		
		return MAPPER.readValues(p, valueType);
	}
	
	public static <T> MappingIterator<T> readValues(JsonParser p, TypeReference<T> valueTypeRef) throws IOException {
		
		return MAPPER.readValues(p, valueTypeRef);
	}
	
	public static JsonNode readTree(InputStream in) throws IOException {
		
		return MAPPER.readTree(in);
	}
	
	public static JsonNode readTree(Reader r) throws IOException {
		
		return MAPPER.readTree(r);
	}
	
	public static JsonNode readTree(String content) throws JsonProcessingException, JsonMappingException {
		
		return MAPPER.readTree(content);
	}
	
	public static JsonNode readTree(byte[] content) throws IOException {
		
		return MAPPER.readTree(content);
	}
	
	public static JsonNode readTree(byte[] content, int offset, int len) throws IOException {
		
		return MAPPER.readTree(content, offset, len);
	}
	
	public static JsonNode readTree(File file) throws IOException {
		
		return MAPPER.readTree(file);
	}
	
	public static void writeValue(JsonGenerator g, Object value) throws IOException, StreamWriteException, DatabindException {
		
		MAPPER.writeValue(g, value);
	}
	
	public static void writeTree(JsonGenerator g, TreeNode rootNode) throws IOException {
		
		MAPPER.writeTree(g, rootNode);
	}
	
	public static void writeTree(JsonGenerator g, JsonNode rootNode) throws IOException {
		
		MAPPER.writeTree(g, rootNode);
	}
	
	public static ObjectNode createObjectNode() {
		
		return MAPPER.createObjectNode();
	}
	
	public static ArrayNode createArrayNode() {
		
		return MAPPER.createArrayNode();
	}
	
	public static JsonNode missingNode() {
		
		return MAPPER.missingNode();
	}
	
	public static JsonNode nullNode() {
		
		return MAPPER.nullNode();
	}
	
	public static JsonParser treeAsTokens(TreeNode n) {
		
		return MAPPER.treeAsTokens(n);
	}
	
	public static <T> T treeToValue(TreeNode n, Class<T> valueType) throws IllegalArgumentException, JsonProcessingException {
		
		return MAPPER.treeToValue(n, valueType);
	}
	
	public static <T> T treeToValue(TreeNode n, JavaType valueType) throws IllegalArgumentException, JsonProcessingException {
		
		return MAPPER.treeToValue(n, valueType);
	}
	
	public static <T> T treeToValue(TreeNode n, TypeReference<T> toValueTypeRef)
			throws IllegalArgumentException, JsonProcessingException {
		
		return MAPPER.treeToValue(n, toValueTypeRef);
	}
	
	public static <T extends JsonNode> T valueToTree(Object fromValue) throws IllegalArgumentException {
		
		return MAPPER.valueToTree(fromValue);
	}
	
	public static <T> T readValue(File src, Class<T> valueType) throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(src, valueType);
	}
	
	public static <T> T readValue(File src, TypeReference<T> valueTypeRef)
			throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(src, valueTypeRef);
	}
	
	public static <T> T readValue(File src, JavaType valueType) throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(src, valueType);
	}
	
	public static <T> T readValue(String content, Class<T> valueType) throws JsonProcessingException, JsonMappingException {
		
		return MAPPER.readValue(content, valueType);
	}
	
	public static <T> T readValue(String content, TypeReference<T> valueTypeRef)
			throws JsonProcessingException, JsonMappingException {
		
		return MAPPER.readValue(content, valueTypeRef);
	}
	
	public static <T> T readValue(String content, JavaType valueType) throws JsonProcessingException, JsonMappingException {
		
		return MAPPER.readValue(content, valueType);
	}
	
	public static <T> T readValue(Reader src, Class<T> valueType) throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(src, valueType);
	}
	
	public static <T> T readValue(Reader src, TypeReference<T> valueTypeRef)
			throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(src, valueTypeRef);
	}
	
	public static <T> T readValue(Reader src, JavaType valueType) throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(src, valueType);
	}
	
	public static <T> T readValue(InputStream src, Class<T> valueType)
			throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(src, valueType);
	}
	
	public static <T> T readValue(InputStream src, TypeReference<T> valueTypeRef)
			throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(src, valueTypeRef);
	}
	
	public static <T> T readValue(InputStream src, JavaType valueType)
			throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(src, valueType);
	}
	
	public static <T> T readValue(byte[] src, Class<T> valueType) throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(src, valueType);
	}
	
	public static <T> T readValue(byte[] src, int offset, int len, Class<T> valueType)
			throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(src, offset, len, valueType);
	}
	
	public static <T> T readValue(byte[] src, TypeReference<T> valueTypeRef)
			throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(src, valueTypeRef);
	}
	
	public static <T> T readValue(byte[] src, int offset, int len, TypeReference<T> valueTypeRef)
			throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(src, offset, len, valueTypeRef);
	}
	
	public static <T> T readValue(byte[] src, JavaType valueType) throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(src, valueType);
	}
	
	public static <T> T readValue(byte[] src, int offset, int len, JavaType valueType)
			throws IOException, StreamReadException, DatabindException {
		
		return MAPPER.readValue(src, offset, len, valueType);
	}
	
	public static <T> T readValue(DataInput src, Class<T> valueType) throws IOException {
		
		return MAPPER.readValue(src, valueType);
	}
	
	public static <T> T readValue(DataInput src, JavaType valueType) throws IOException {
		
		return MAPPER.readValue(src, valueType);
	}
	
	public static void writeValue(File resultFile, Object value) throws IOException, StreamWriteException, DatabindException {
		
		MAPPER.writeValue(resultFile, value);
	}
	
	public static void writeValue(OutputStream out, Object value) throws IOException, StreamWriteException, DatabindException {
		
		MAPPER.writeValue(out, value);
	}
	
	public static void writeValue(DataOutput out, Object value) throws IOException {
		
		MAPPER.writeValue(out, value);
	}
	
	public static void writeValue(Writer w, Object value) throws IOException, StreamWriteException, DatabindException {
		
		MAPPER.writeValue(w, value);
	}
	
	public static String writeValueAsString(Object value) throws JsonProcessingException {
		
		return MAPPER.writeValueAsString(value);
	}
	
	public static byte[] writeValueAsBytes(Object value) throws JsonProcessingException {
		
		return MAPPER.writeValueAsBytes(value);
	}
	
	public static ObjectWriter writer() {
		
		return MAPPER.writer();
	}
	
	public static ObjectWriter writer(SerializationFeature feature) {
		
		return MAPPER.writer(feature);
	}
	
	public static ObjectWriter writer(SerializationFeature first, SerializationFeature... other) {
		
		return MAPPER.writer(first, other);
	}
	
	public static ObjectWriter writer(DateFormat df) {
		
		return MAPPER.writer(df);
	}
	
	public static ObjectWriter writerWithView(Class<?> serializationView) {
		
		return MAPPER.writerWithView(serializationView);
	}
	
	public static ObjectWriter writerFor(Class<?> rootType) {
		
		return MAPPER.writerFor(rootType);
	}
	
	public static ObjectWriter writerFor(TypeReference<?> rootType) {
		
		return MAPPER.writerFor(rootType);
	}
	
	public static ObjectWriter writerFor(JavaType rootType) {
		
		return MAPPER.writerFor(rootType);
	}
	
	public static ObjectWriter writer(PrettyPrinter pp) {
		
		return MAPPER.writer(pp);
	}
	
	public static ObjectWriter writerWithDefaultPrettyPrinter() {
		
		return MAPPER.writerWithDefaultPrettyPrinter();
	}
	
	public static ObjectWriter writer(FilterProvider filterProvider) {
		
		return MAPPER.writer(filterProvider);
	}
	
	public static ObjectWriter writer(FormatSchema schema) {
		
		return MAPPER.writer(schema);
	}
	
	public static ObjectWriter writer(Base64Variant defaultBase64) {
		
		return MAPPER.writer(defaultBase64);
	}
	
	public static ObjectWriter writer(CharacterEscapes escapes) {
		
		return MAPPER.writer(escapes);
	}
	
	public static ObjectWriter writer(ContextAttributes attrs) {
		
		return MAPPER.writer(attrs);
	}

	
	public static ObjectReader reader() {
		
		return MAPPER.reader();
	}
	
	public static ObjectReader reader(DeserializationFeature feature) {
		
		return MAPPER.reader(feature);
	}
	
	public static ObjectReader reader(DeserializationFeature first, DeserializationFeature... other) {
		
		return MAPPER.reader(first, other);
	}
	
	public static ObjectReader readerForUpdating(Object valueToUpdate) {
		
		return MAPPER.readerForUpdating(valueToUpdate);
	}
	
	public static ObjectReader readerFor(JavaType type) {
		
		return MAPPER.readerFor(type);
	}
	
	public static ObjectReader readerFor(Class<?> type) {
		
		return MAPPER.readerFor(type);
	}
	
	public static ObjectReader readerFor(TypeReference<?> typeRef) {
		
		return MAPPER.readerFor(typeRef);
	}
	
	public static ObjectReader readerForArrayOf(Class<?> type) {
		
		return MAPPER.readerForArrayOf(type);
	}
	
	public static ObjectReader readerForListOf(Class<?> type) {
		
		return MAPPER.readerForListOf(type);
	}
	
	public static ObjectReader readerForMapOf(Class<?> type) {
		
		return MAPPER.readerForMapOf(type);
	}
	
	public static ObjectReader reader(JsonNodeFactory nodeFactory) {
		
		return MAPPER.reader(nodeFactory);
	}
	
	public static ObjectReader reader(FormatSchema schema) {
		
		return MAPPER.reader(schema);
	}
	
	public static ObjectReader reader(InjectableValues injectableValues) {
		
		return MAPPER.reader(injectableValues);
	}
	
	public static ObjectReader readerWithView(Class<?> view) {
		
		return MAPPER.readerWithView(view);
	}
	
	public static ObjectReader reader(Base64Variant defaultBase64) {
		
		return MAPPER.reader(defaultBase64);
	}
	
	public static ObjectReader reader(ContextAttributes attrs) {
		
		return MAPPER.reader(attrs);
	}
	
	
	public static <T> T convertValue(Object fromValue, Class<T> toValueType) throws IllegalArgumentException {
		
		return MAPPER.convertValue(fromValue, toValueType);
	}
	
	public static <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) throws IllegalArgumentException {
		
		return MAPPER.convertValue(fromValue, toValueTypeRef);
	}
	
	public static <T> T convertValue(Object fromValue, JavaType toValueType) throws IllegalArgumentException {
		
		return MAPPER.convertValue(fromValue, toValueType);
	}
	
	
	public static <T> T updateValue(T valueToUpdate, Object overrides) throws JsonMappingException {
		
		return MAPPER.updateValue(valueToUpdate, overrides);
	}
	
	
	public static void acceptJsonFormatVisitor(Class<?> type, JsonFormatVisitorWrapper visitor) throws JsonMappingException {
		
		MAPPER.acceptJsonFormatVisitor(type, visitor);
	}
	
	public static void acceptJsonFormatVisitor(JavaType type, JsonFormatVisitorWrapper visitor) throws JsonMappingException {
		
		MAPPER.acceptJsonFormatVisitor(type, visitor);
	}
	
	public static void clearCaches() {
		
		MAPPER.clearCaches();
	}
	
	
}

