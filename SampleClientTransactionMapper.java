/**
 * MES 프로젝트 내 Mapper
 */
@Mapper
public interface SampleClientTransactionMapper {

    List<Map<String, Object>> sampleFindFunction(Map<String, Object> param);
}
