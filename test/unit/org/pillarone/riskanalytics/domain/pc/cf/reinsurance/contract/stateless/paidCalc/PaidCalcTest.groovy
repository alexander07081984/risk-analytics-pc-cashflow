package org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.paidCalc

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.pillarone.riskanalytics.domain.pc.cf.claim.ClaimCashflowPacket
import org.pillarone.riskanalytics.core.simulation.IPeriodCounter
import org.pillarone.riskanalytics.domain.pc.cf.claim.GrossClaimRoot
import org.pillarone.riskanalytics.domain.pc.cf.pattern.PatternPacket
import org.pillarone.riskanalytics.domain.pc.cf.claim.IClaimRoot
import org.pillarone.riskanalytics.domain.pc.cf.claim.ClaimRoot
import org.pillarone.riskanalytics.domain.pc.cf.claim.ClaimType
import org.pillarone.riskanalytics.core.simulation.engine.PeriodScope
import org.pillarone.riskanalytics.core.simulation.TestPeriodScopeUtilities
import org.pillarone.riskanalytics.core.simulation.TestPeriodCounterUtilities
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.IPaidCalculation
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.paidImpl.TermPaidRespectIncurredByClaim
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.filterUtilities.RIUtilities
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.LayerParameters
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.APBasis
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.PeriodLayerParameters
import org.pillarone.riskanalytics.domain.pc.cf.pattern.PatternPacketTests
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.ContractCoverBase

/**
 *   author simon.parten @ art-allianz . com
 */
class PaidCalcTest extends GroovyTestCase {

    DateTime start2010 = new DateTime(2010, 1, 1, 1, 0, 0, 0, DateTimeZone.UTC)
    DateTime start2011 = new DateTime(2011, 1, 1, 1, 0, 0, 0, DateTimeZone.UTC)
    DateTime start2012 = new DateTime(2012, 1, 1, 1, 0, 0, 0, DateTimeZone.UTC)
    List<ClaimCashflowPacket> grossClaims = new ArrayList<ClaimCashflowPacket>()

    protected void setUp() {

        IPeriodCounter counter = TestPeriodCounterUtilities.getLimitedContinuousPeriodCounter(start2010, 4)
        GrossClaimRoot grossClaimRoot1 = getGrossClaim([5i, 13i], [0.5d, 1d], -100, start2010, start2010, start2010) /* 50 + 50 */
        GrossClaimRoot grossClaimRoot2 = getGrossClaim([5i, 13i], [0.5d, 1d], -200, start2010, start2010, start2010) /* 100 + 100 */
        GrossClaimRoot grossClaimRoot3 = getGrossClaim([5i, 13i], [0.5d, 1d], -100, start2010, start2011, start2011) /* 50 + 50 */
        GrossClaimRoot grossClaimRoot4 = getGrossClaim([5i, 13i], [0.5d, 1d], -300, start2010, start2011, start2011) /* 150 + 150 */

        List<GrossClaimRoot> rooClaims = new ArrayList<GrossClaimRoot>()
        rooClaims << grossClaimRoot1
        rooClaims << grossClaimRoot2
        rooClaims << grossClaimRoot3
        rooClaims << grossClaimRoot4

        List<ClaimCashflowPacket> allClaims = new ArrayList<ClaimCashflowPacket>()
        for (int i = 0; i < 5; i++) {
            for (GrossClaimRoot aRoot in rooClaims) {
                allClaims.addAll(aRoot.getClaimCashflowPackets(counter))
            }
            counter.next()
        }

        grossClaims.clear()
        grossClaims.addAll(allClaims)

    }

    private GrossClaimRoot getGrossClaim(List<Integer> patternMonths, List<Double> pattern, double ultimate, DateTime exposureStart, DateTime patternStart, DateTime occurenceDate) {
        PatternPacket patternPacket = PatternPacketTests.getPattern(patternMonths, pattern, false)
        IClaimRoot claimRoot = new ClaimRoot(ultimate, ClaimType.SINGLE, exposureStart, occurenceDate)
        GrossClaimRoot grossClaimRoot = new GrossClaimRoot(claimRoot, patternPacket, patternStart)
        return grossClaimRoot
    }


    void testLayerCededPaid() {

        IPaidCalculation calculation = new TermPaidRespectIncurredByClaim()
        List<ClaimCashflowPacket> period1Claims = RIUtilities.cashflowsByIncurredDate(start2010, start2011.minusMillis(1), grossClaims, ContractCoverBase.LOSSES_OCCURING)

        List<ClaimCashflowPacket> latestCashflows = RIUtilities.latestCashflowByIncurredClaim(period1Claims)
        LayerParameters periodLimitAndDeductible = new LayerParameters(1, 60, 90)
        periodLimitAndDeductible.addAdditionalPremium(10, 0, 0, APBasis.LOSS)
        double cededPaid = calculation.layerCededPaid(latestCashflows, periodLimitAndDeductible)
        assert cededPaid == 40 + 90 - 10

    }

    void testCumulativePaidRespectTerm() {
        IPaidCalculation calculation = new TermPaidRespectIncurredByClaim()
        PeriodScope periodScope = TestPeriodScopeUtilities.getPeriodScope(start2010, 3)
        List<ClaimCashflowPacket> period1Claims = RIUtilities.cashflowsByIncurredDate(start2010, start2011.minusMillis(1), grossClaims, ContractCoverBase.LOSSES_OCCURING)
        List<ClaimCashflowPacket> period1Claims2010 = period1Claims.findAll {it -> it.getDate().isBefore(start2011) }

        PeriodLayerParameters allLayers = new PeriodLayerParameters()
        allLayers.add(1, 1, 1, 60, 90, 0, 0, 0, APBasis.LOSS)
        Map<Integer, Double> period1Calc = calculation.cededCumulativePaidRespectTerm(period1Claims2010, allLayers, periodScope, ContractCoverBase.LOSSES_OCCURING, 240, 10)

//        40 ceded out of claim limit, minus 10 term ded
        assertEquals("p1", 30, period1Calc.get(0))

        periodScope.prepareNextPeriod()
        List<ClaimCashflowPacket> period1and2Claims = RIUtilities.cashflowsByIncurredDate(start2010, start2012.minusMillis(1), grossClaims, ContractCoverBase.LOSSES_OCCURING)
        List<ClaimCashflowPacket> period1and2ClaimsBefore2012 = period1and2Claims.findAll {it -> it.getDate().isBefore(start2012) }

        Map<Integer, Double> period2Calc = calculation.cededCumulativePaidRespectTerm(period1and2ClaimsBefore2012, allLayers, periodScope, ContractCoverBase.LOSSES_OCCURING, 240, 10)
        assertEquals("p1", 120 , period2Calc.get(0) )
        assertEquals("p2", 90 , period2Calc.get(1) )

        periodScope.prepareNextPeriod()
        Map<Integer, Double> p3Calc = calculation.cededCumulativePaidRespectTerm(grossClaims, allLayers, periodScope, ContractCoverBase.LOSSES_OCCURING, 240, 10)
        assertEquals("p1", 120 , p3Calc.get(0) )
        assertEquals("p2", 120 , p3Calc.get(1) )

        GrossClaimRoot shouldCede0 = getGrossClaim([5i, 13i], [0.5d, 1d], -100, start2011, start2012, start2012) /* 50 + 50 */
        periodScope.prepareNextPeriod()
        grossClaims.addAll( shouldCede0.getClaimCashflowPackets(periodScope.getPeriodCounter()) )
        Map<Integer, Double> p4Calc = calculation.cededCumulativePaidRespectTerm(grossClaims, allLayers, periodScope, ContractCoverBase.LOSSES_OCCURING, 240, 10)
        assertEquals("p1", 120 , p4Calc.get(0) )
        assertEquals("p2", 120 , p4Calc.get(1) )
        assertEquals("p3", 0 , p4Calc.get(2) )




    }

    void testPaidLossAllLayers() {
        IPaidCalculation calculation = new TermPaidRespectIncurredByClaim()
        List<ClaimCashflowPacket> period1Claims = RIUtilities.cashflowsByIncurredDate(start2010, start2011.minusMillis(1), grossClaims, ContractCoverBase.LOSSES_OCCURING)
        List<ClaimCashflowPacket> latestCashflows = RIUtilities.latestCashflowByIncurredClaim(period1Claims)


        LayerParameters claimLimits = new LayerParameters(1, 60, 90)
        claimLimits.addAdditionalPremium(0, 0, 0, APBasis.LOSS)

        LayerParameters moreClaimLimits = new LayerParameters(0.5, 150, 40)
        moreClaimLimits.addAdditionalPremium(0, 0, 0, APBasis.LOSS)

        List<LayerParameters> allLayers = [claimLimits, moreClaimLimits]


        double cededPaid = calculation.paidLossAllLayers(latestCashflows, allLayers)
        assert cededPaid == 40 + 90 + 40 * 0.5

    }

    void testAdditionalPremiumByLayer() {

    }
}