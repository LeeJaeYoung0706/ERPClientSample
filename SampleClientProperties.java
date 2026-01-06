/**
 * 민감 정보
 * @param sampleKey
 * @param baseUrl
 * @param hashKey
 * @param token
 * @param calleName
 * @param groupSeq
 */
@ConfigurationProperties(prefix = "sample.client")
public record SampleClientProperties(
        String sampleKey,
        String baseUrl,
        String hashKey,
        String token,
        String calleName,
        String groupSeq
) {
}
