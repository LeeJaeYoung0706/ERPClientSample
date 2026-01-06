@Configuration
public class ObjectMapperConfig {

    /**
     * 외부용 ObjectMapper
     *
     * @return
     */
    @Bean("externalObjectMapper")
    public ObjectMapper externalObjectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder
                .createXmlMapper(false)
                .build()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
