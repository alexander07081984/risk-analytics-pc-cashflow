package org.pillarone.riskanalytics.domain.pc.cf.reserve.updating.aggregate;

import org.joda.time.DateTime;
import org.pillarone.riskanalytics.core.parameterization.AbstractParameterObject;
import org.pillarone.riskanalytics.core.parameterization.ConstrainedMultiDimensionalParameter;
import org.pillarone.riskanalytics.core.parameterization.IParameterObjectClassifier;
import org.pillarone.riskanalytics.core.simulation.IPeriodCounter;
import org.pillarone.riskanalytics.core.simulation.engine.PeriodScope;
import org.pillarone.riskanalytics.domain.pc.cf.claim.ClaimRoot;
import org.pillarone.riskanalytics.domain.pc.cf.claim.GrossClaimRoot;
import org.pillarone.riskanalytics.domain.pc.cf.pattern.PatternPacket;
import org.pillarone.riskanalytics.domain.utils.InputFormatConverter;
import org.pillarone.riskanalytics.domain.utils.datetime.DateTimeUtilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author stefan.kunz (at) intuitive-collaboration (dot) com
 */
public class AggregateActualClaimsStrategy extends AbstractParameterObject implements IAggregateActualClaimsStrategy {

    private ConstrainedMultiDimensionalParameter history = getDefaultHistory();
    private PayoutPatternBase payoutPatternBase = PayoutPatternBase.PERIOD_START_DATE;

    public IParameterObjectClassifier getType() {
        return AggregateActualClaimsStrategyType.AGGREGATE;
    }

    public Map getParameters() {
        Map<String, Object> parameters = new HashMap<String, Object>(2);
        parameters.put("history", history);
        parameters.put("payoutPatternBase", payoutPatternBase);
        return parameters;
    }

    public static ConstrainedMultiDimensionalParameter getDefaultHistory() {
        return AggregateHistoricClaimsConstraints.getDefault();
    }

    private Map<Integer, AggregateHistoricClaim> historicClaimsPerContractPeriod;

    /**
     * This function has to be called during period 0 in order to fill the periodStore correctly! The content of the
     * reported amount column is ignored. historicClaimsPerContractPeriod is filled.
     *
     * @param periodCounter required for date calculations
     * @param updateDate    all reported claims after the updateDate are ignored
     */
    public void lazyInitHistoricClaimsPerContractPeriod(IPeriodCounter periodCounter, DateTime updateDate) {
        // key: contractPeriod
        if (historicClaimsPerContractPeriod == null) {
            historicClaimsPerContractPeriod = new HashMap<Integer, AggregateHistoricClaim>();
            for (int row = history.getTitleRowCount(); row < history.getRowCount(); row++) {
                DateTime reportedDate = (DateTime) history.getValueAt(row, AggregateHistoricClaimsConstraints.REPORT_DATE_INDEX);
                if (reportedDate.isAfter(updateDate)) continue; // ignore any claim update after updateDate
                Integer contractPeriod = InputFormatConverter.getInt(history.getValueAt(row, AggregateHistoricClaimsConstraints.CONTRACT_PERIOD_INDEX)) - 1; // -1 : difference between UI and internal sight
                // as this method is called always for initialization in the first iteration and period
                // iterationStore access without arguments is always reading and writing to period 0
                AggregateHistoricClaim claim = historicClaimsPerContractPeriod.get(contractPeriod);
                if (claim == null) {
                    claim = new AggregateHistoricClaim(contractPeriod, periodCounter, payoutPatternBase);
                    historicClaimsPerContractPeriod.put(contractPeriod, claim);
                }
                double cumulativePaid = InputFormatConverter.getDouble(history.getValueAt(row, AggregateHistoricClaimsConstraints.PAID_AMOUNT_INDEX));
                double cumulativeReported = InputFormatConverter.getDouble(history.getValueAt(row, AggregateHistoricClaimsConstraints.REPORTED_AMOUNT_INDEX));
                claim.add(reportedDate, cumulativeReported, cumulativePaid);
            }
        }
    }

    public void checkClaimRootOccurenceAgainstFirstActualPaid(List<ClaimRoot> baseClaims, int contractPeriod, IPeriodCounter periodCounter, DateTime updateDate) {
        lazyInitHistoricClaimsPerContractPeriod(periodCounter, updateDate);
        if (baseClaims.size() != 1) {
            throw new RuntimeException("not one base stochastic claim in aggregate actual claims strategy, contact development");
        }
        AggregateHistoricClaim aggregateHistoricClaim = historicClaimsPerContractPeriod.get(contractPeriod);
        if (aggregateHistoricClaim == null) return;
        ClaimRoot claimRoot = baseClaims.get(0);
//        Known payments before the exposure start date cannot be real. Invent a new root claim with a new exposure start date. THis is a rare case, in general
//        this method should do nothing!
        if (aggregateHistoricClaim.firstActualPaidDateOrNull() != null && claimRoot.getExposureStartDate().isAfter(aggregateHistoricClaim.firstActualPaidDateOrNull())) {
            ClaimRoot claimRoot1 = new ClaimRoot(claimRoot.getUltimate(), claimRoot.getClaimType(), aggregateHistoricClaim.firstActualPaidDateOrNull(), claimRoot.getOccurrenceDate());
            baseClaims.clear();
            baseClaims.add(claimRoot1);
        }

    }

    /**
     * Creates a GrossClaimRoot. It's payout pattern is modified if the current period end is before the update date and
     * there exists a AggregateHistoricClaim for contractPeriod.
     *
     * @param claimRoot      providing the ultimate and occurrence date
     * @param contractPeriod
     * @param payoutPattern  original payout pattern
     * @param periodScope
     * @param updateDate     @return a GrossClaimRoot with a possibly modified payoutPattern
     * @param days360
     * @param sanityChecks
     */
    public GrossClaimRoot claimWithAdjustedPattern(ClaimRoot claimRoot, int contractPeriod, PatternPacket payoutPattern,
                                                   PeriodScope periodScope, DateTime updateDate, DateTimeUtilities.Days360 days360, boolean sanityChecks) {
//        If the update date is the start of the first period this is an inception model, simply proceed without updating effects.
        if (updateDate.equals(periodScope.getPeriodCounter().startOfFirstPeriod())) {
            return new GrossClaimRoot(claimRoot, payoutPattern);
        }
        lazyInitHistoricClaimsPerContractPeriod(periodScope.getPeriodCounter(), updateDate);
        AggregateHistoricClaim historicClaim = historicClaimsPerContractPeriod.get(contractPeriod);

//        If we have claim updates, rescale the payment pattern against them.
        if (historicClaim != null) {
            if(!historicClaim.noUpdates()) {
                return historicClaim.claimWithAdjustedPattern(payoutPattern, claimRoot, updateDate, days360, sanityChecks);
            }
        }
//        If there are no claim updates, we rescale the pattern to avoid stochastic payments before update date.
        PatternPacket patternPacket = payoutPattern.rescalePatternToUpdateDate(updateDate, claimRoot.getExposureStartDate(), false);
        patternPacket.consistencyCheck(sanityChecks, sanityChecks, sanityChecks, sanityChecks);
        return new GrossClaimRoot(claimRoot, patternPacket);
    }

    public AggregateHistoricClaim historicClaims(int period, IPeriodCounter periodCounter, DateTime updateDate) {
        lazyInitHistoricClaimsPerContractPeriod(periodCounter, updateDate);
        AggregateHistoricClaim historicClaim = historicClaimsPerContractPeriod.get(period);
        if (historicClaim == null) {
            historicClaim = new AggregateHistoricClaim(period, periodCounter, PayoutPatternBase.CLAIM_OCCURANCE_DATE);
        }
        return historicClaim;
    }
}
