package models.gira

import org.pillarone.riskanalytics.core.model.StochasticModel
import org.pillarone.riskanalytics.core.simulation.IPeriodCounter
import org.pillarone.riskanalytics.core.simulation.LimitedContinuousPeriodCounter


import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.joda.time.DateTime
import org.joda.time.Period
import org.pillarone.riskanalytics.domain.pc.cf.dependency.Dependencies
import org.pillarone.riskanalytics.domain.pc.cf.dependency.EventGenerators
import org.pillarone.riskanalytics.domain.pc.cf.exposure.RiskBands
import org.pillarone.riskanalytics.domain.pc.cf.exposure.UnderwritingSegments
import org.pillarone.riskanalytics.domain.pc.cf.global.GlobalParameters
import org.pillarone.riskanalytics.domain.pc.cf.indexing.Indices
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.ReinsuranceContracts
import org.pillarone.riskanalytics.domain.pc.cf.segment.Segments
import org.pillarone.riskanalytics.domain.pc.cf.pattern.Patterns
import org.pillarone.riskanalytics.domain.pc.cf.claim.generator.ClaimsGenerators
import org.pillarone.riskanalytics.domain.pc.cf.pattern.Pattern
import org.pillarone.riskanalytics.domain.pc.cf.pattern.IReportingPatternMarker
import org.pillarone.riskanalytics.domain.pc.cf.pattern.PayoutReportingCombinedPattern
import org.pillarone.riskanalytics.domain.pc.cf.claim.generator.ClaimsGenerator
import org.pillarone.riskanalytics.domain.pc.cf.pattern.IPayoutPatternMarker
import org.pillarone.riskanalytics.domain.pc.cf.pattern.IPremiumPatternMarker
import org.pillarone.riskanalytics.domain.pc.cf.reserve.ReservesGenerators
import org.pillarone.riskanalytics.domain.pc.cf.legalentity.LegalEntities

/**
 * @author stefan.kunz (at) intuitive-collaboration (dot) com
 */
class GIRAModel extends StochasticModel {

    private static Log LOG = LogFactory.getLog(GIRAModel);

    GlobalParameters globalParameters
    Indices indices
    Patterns patterns
    UnderwritingSegments underwritingSegments
    ClaimsGenerators claimsGenerators
    ReservesGenerators reservesGenerators
    Dependencies dependencies
    EventGenerators eventGenerators
    LegalEntities legalEntities
    Segments segments
    ReinsuranceContracts reinsuranceContracts

    @Override
    void initComponents() {
        globalParameters = new GlobalParameters()
        underwritingSegments = new UnderwritingSegments()
        indices = new Indices()
        patterns = new Patterns()
        claimsGenerators = new ClaimsGenerators()
        reservesGenerators = new ReservesGenerators()
        dependencies = new Dependencies()
        eventGenerators = new EventGenerators()
        legalEntities = new LegalEntities()
        segments = new Segments()
        reinsuranceContracts = new ReinsuranceContracts()

        addStartComponent patterns
        addStartComponent dependencies
        addStartComponent eventGenerators
    }

    @Override
    void wireComponents() {
        underwritingSegments.inFactors = indices.outFactors
        underwritingSegments.inPatterns = patterns.outPatterns
        claimsGenerators.inFactors = indices.outFactors
        claimsGenerators.inPatterns = patterns.outPatterns
        claimsGenerators.inUnderwritingInfo = underwritingSegments.outUnderwritingInfo
        claimsGenerators.inEventSeverities = dependencies.outEventSeverities
        claimsGenerators.inEventSeverities = eventGenerators.outEventSeverities
        claimsGenerators.inEventFrequencies = eventGenerators.outEventFrequencies
        reservesGenerators.inFactors= indices.outFactors
        reservesGenerators.inPatterns = patterns.outPatterns
        indices.inEventSeverities = dependencies.outEventSeverities
        if (segments.subComponentCount() == 0) {
            reinsuranceContracts.inClaims = claimsGenerators.outClaims
            reinsuranceContracts.inUnderwritingInfo = underwritingSegments.outUnderwritingInfo
        }
        else {
            segments.inClaims = claimsGenerators.outClaims
            segments.inUnderwritingInfo = underwritingSegments.outUnderwritingInfo
            reinsuranceContracts.inClaims = segments.outClaimsGross
            reinsuranceContracts.inUnderwritingInfo = segments.outUnderwritingInfoGross
            segments.inClaimsCeded = reinsuranceContracts.outClaimsCeded
            segments.inUnderwritingInfoCeded = reinsuranceContracts.outUnderwritingInfoCeded
        }
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
        for (Pattern pattern: patterns.subPayoutPatterns.componentList) {
            Period period = pattern.parmPattern.getPattern(IPayoutPatternMarker.class).getLastCumulativePeriod()
            LOG.debug("payout pattern $pattern.name $period.months")
            claimsGeneratorPatternLengths.put(pattern.name, period)
        }
        for (Pattern pattern: patterns.subReportingPatterns.componentList) {
            Period period = pattern.parmPattern.getPattern(IReportingPatternMarker.class).getLastCumulativePeriod()
            LOG.debug("reporting pattern $pattern.name $period.months")
            claimsGeneratorPatternLengths.put(pattern.name, period)
        }
        for (PayoutReportingCombinedPattern pattern: patterns.subPayoutAndReportingPatterns.componentList) {
            Period period = pattern.parmPattern.getPayoutPattern().getLastCumulativePeriod()
            LOG.debug("combined payout reporting pattern $pattern.name $period.months")
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
            Period period = pattern.parmPattern.getPattern(IPremiumPatternMarker.class).getLastCumulativePeriod()
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
