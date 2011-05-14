package models.nonLifeCashflow

import org.joda.time.DateTime
import org.joda.time.Period
import org.pillarone.riskanalytics.core.model.StochasticModel
import org.pillarone.riskanalytics.core.simulation.IPeriodCounter
import org.pillarone.riskanalytics.core.simulation.LimitedContinuousPeriodCounter
import org.pillarone.riskanalytics.domain.pc.cf.claim.generator.ClaimsGenerator
import org.pillarone.riskanalytics.domain.pc.cf.claim.generator.ClaimsGenerators
import org.pillarone.riskanalytics.domain.pc.cf.exposure.UnderwritingSegments
import org.pillarone.riskanalytics.domain.pc.cf.global.GlobalParameters
import org.pillarone.riskanalytics.domain.pc.cf.indexing.Indices
import org.pillarone.riskanalytics.domain.pc.cf.pattern.Pattern
import org.pillarone.riskanalytics.domain.pc.cf.pattern.Patterns
import org.pillarone.riskanalytics.domain.pc.cf.dependency.Dependencies
import org.pillarone.riskanalytics.domain.pc.cf.dependency.MultipleDependencies
import org.pillarone.riskanalytics.domain.pc.cf.exposure.RiskBands
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

/**
 * @author stefan.kunz (at) intuitive-collaboration (dot) com
 */
class NonLifeCashflowModel extends StochasticModel {

    private static Log LOG = LogFactory.getLog(NonLifeCashflowModel);

    GlobalParameters globalParameters
    Indices indices
    Patterns patterns
    UnderwritingSegments underwritingSegments
    ClaimsGenerators claimsGenerators
    Dependencies dependencies
    MultipleDependencies multipleDependencies

    @Override
    void initComponents() {
        globalParameters = new GlobalParameters()
        underwritingSegments = new UnderwritingSegments()
        indices = new Indices()
        patterns = new Patterns()
        claimsGenerators = new ClaimsGenerators()
        dependencies = new Dependencies()
        multipleDependencies = new MultipleDependencies()

        addStartComponent indices
        addStartComponent patterns
        addStartComponent dependencies
        addStartComponent multipleDependencies
    }

    @Override
    void wireComponents() {
        underwritingSegments.inFactors = indices.outFactors
        underwritingSegments.inPatterns = patterns.outPatterns
        claimsGenerators.inFactors = indices.outFactors
        claimsGenerators.inPatterns = patterns.outPatterns
        claimsGenerators.inUnderwritingInfo = underwritingSegments.outUnderwritingInfo
        claimsGenerators.inEventSeverities = dependencies.outEventSeverities
        claimsGenerators.inEventSeverities = multipleDependencies.outEventSeverities
        claimsGenerators.inEventFrequencies = multipleDependencies.outEventFrequencies
    }

    @Override
    IPeriodCounter createPeriodCounter(DateTime beginOfFirstPeriod) {
        Period developmentPeriod = lastPatternPeriod()
        int numberOfYears = Math.max(1, Math.ceil(developmentPeriod.months / 12d) + 1)
        return new LimitedContinuousPeriodCounter(globalParameters.parmProjectionStartDate, Period.years(1), numberOfYears)
    }

    private Period lastPatternPeriod() {
        Period maxPeriods = Period.months(0);

        Map<String, Period> claimsGeneratorPatternLengths = new HashMap<String, Period>()
        for (Pattern pattern: patterns.subPayoutPatterns.componentList + patterns.subReportingPatterns.componentList) {
            Period period = pattern.parmPattern.pattern.getLastCumulativePeriod()
            LOG.debug("payout/reporting pattern $pattern.name $period.months")
            claimsGeneratorPatternLengths.put(pattern.name, period)
        }

        if (!claimsGeneratorPatternLengths.isEmpty()) {
            for (ClaimsGenerator generator: claimsGenerators.componentList) {
                Period period = claimsGeneratorPatternLengths.get(generator.parmPayoutPattern?.stringValue)
                if (period != null) {
                    maxPeriods = Period.months(Math.max(maxPeriods.months, period.months))
                }
                period = claimsGeneratorPatternLengths.get(generator.parmReportingPattern?.stringValue)
                if (period != null) {
                    maxPeriods = Period.months(Math.max(maxPeriods.months, period.months))
                }
            }
        }

        Map<String, Period> premiumPatternLengths = new HashMap<String, Period>()
        for (Pattern pattern: patterns.subPremiumPatterns.componentList) {
            Period period = pattern.parmPattern.pattern.getLastCumulativePeriod()
            LOG.debug("premium pattern $pattern.name $period.months")
            premiumPatternLengths.put(pattern.name, period)
        }
        if (!premiumPatternLengths.isEmpty()) {
            for (RiskBands riskBands: underwritingSegments.componentList) {
                Period period = premiumPatternLengths.get(riskBands.parmPremiumPattern?.stringValue)
                if (period != null) {
                    maxPeriods = Period.months(Math.max(maxPeriods.months, period.months))
                }
            }
        }
        LOG.debug("max periods: $maxPeriods")
        return maxPeriods
    }

    public int maxNumberOfFullyDistinctPeriods() {
        1
    }
}
