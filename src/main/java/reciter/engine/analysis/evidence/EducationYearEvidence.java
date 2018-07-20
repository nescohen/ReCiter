package reciter.engine.analysis.evidence;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

import lombok.Data;
import lombok.ToString;
import reciter.model.identity.Education;

@Data
@ToString
@DynamoDBDocument
public class EducationYearEvidence {
	private Integer identityBachelorYear;
	private Integer identityDoctoralYear;
	private Integer articleYear;
    private Integer discrepancyDegreeYearBachelor;
    private double discrepancyDegreeYearBachelorScore;
    private Integer discrepancyDegreeYearDoctoral;
    private double discrepancyDegreeYearDoctoralScore;
    
}
