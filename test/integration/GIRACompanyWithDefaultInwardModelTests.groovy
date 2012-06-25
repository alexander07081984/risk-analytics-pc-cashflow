import models.gira.GIRAModel
import org.pillarone.riskanalytics.core.simulation.engine.ModelTest

/**
 * @author stefan.kunz (at) intuitive-collaboration (dot) com
 */
class GIRACompanyWithDefaultInwardModelTests extends ModelTest {

    Class getModelClass() {
        GIRAModel
    }

    @Override
    String getResultConfigurationFileName() {
        'GIRALegalEntityDrillDownResultConfiguration'
    }

    String getResultConfigurationDisplayName() {
        'Legal Entity, Drill Down'
    }

    @Override
    String getParameterFileName() {
        'TestGIRAMultiCompanyWithDefaultInwardParameters'
    }

    String getParameterDisplayName() {
        'Multi Company with Default, Inward'
    }

    @Override
    int getIterationCount() {
        1
    }

    //todo(sku): re-enable net financials currently wrong
//    protected boolean shouldCompareResults() {
//        true
//    }
}
