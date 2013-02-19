package org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.contracts;

import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.pillarone.riskanalytics.core.components.Component;
import org.pillarone.riskanalytics.core.components.IterationStore;
import org.pillarone.riskanalytics.core.components.PeriodStore;
import org.pillarone.riskanalytics.core.packets.PacketList;
import org.pillarone.riskanalytics.core.parameterization.ConstrainedMultiDimensionalParameter;
import org.pillarone.riskanalytics.core.parameterization.ConstraintsFactory;
import org.pillarone.riskanalytics.core.simulation.IPeriodCounter;
import org.pillarone.riskanalytics.core.simulation.SimulationException;
import org.pillarone.riskanalytics.core.simulation.engine.IterationScope;
import org.pillarone.riskanalytics.core.simulation.engine.PeriodScope;
import org.pillarone.riskanalytics.core.simulation.engine.SimulationScope;
import org.pillarone.riskanalytics.core.util.GroovyUtils;
import org.pillarone.riskanalytics.domain.pc.cf.claim.ClaimCashflowPacket;
import org.pillarone.riskanalytics.domain.pc.cf.claim.ICededRoot;
import org.pillarone.riskanalytics.domain.pc.cf.exposure.AllPeriodUnderwritingInfoPacket;
import org.pillarone.riskanalytics.domain.pc.cf.exposure.CededUnderwritingInfoPacket;
import org.pillarone.riskanalytics.domain.pc.cf.exposure.ExposureBase;
import org.pillarone.riskanalytics.domain.pc.cf.exposure.UnderwritingInfoPacket;
import org.pillarone.riskanalytics.domain.pc.cf.exposure.filter.ExposureBaseType;
import org.pillarone.riskanalytics.domain.pc.cf.exposure.filter.IExposureBaseStrategy;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.ContractFinancialsPacket;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.IReinsuranceContractStrategy;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.*;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.additionalPremium.*;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.caching.IAllContractClaimCache;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.caching.UberCacheClaimStore;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.constraints.PremiumSelectionConstraints;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.cover.CoverStrategyType;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.cover.ICoverStrategy;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.filterUtilities.RIUtilities;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.incurredImpl.IncurredAllocation;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.incurredImpl.TermIncurredCalculation;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.paidImpl.ProportionalToGrossPaidAllocation;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.paidImpl.TermPaidRespectIncurredByClaim;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.strategies.NonPropTemplateContractStrategy;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.cover.period.IPeriodStrategy;
import org.pillarone.riskanalytics.domain.utils.marker.IPremiumInfoMarker;
import org.pillarone.riskanalytics.domain.utils.marker.IReinsuranceContractMarker;

import java.util.*;

/**
 * author simon.parten @ art-allianz . com
 */
public class StatelessRIContract extends Component implements IReinsuranceContractMarker {

    private static Log LOG = LogFactory.getLog(StatelessRIContract.class);

    /**
     * Injected by framework
     */
    private IterationScope iterationScope;
    private IterationStore iterationStore;
    private PeriodStore periodStore;
    private PeriodScope periodScope;
    private IPeriodStrategy globalCover;
    private SimulationScope simulationScope;
    private boolean globalSanityChecks;

    /**
     * Incoming information
     */
    private PacketList<ClaimCashflowPacket> inClaims = new PacketList<ClaimCashflowPacket>(ClaimCashflowPacket.class);
    private PacketList<UnderwritingInfoPacket> inUnderwritingInfo = new PacketList<UnderwritingInfoPacket>(UnderwritingInfoPacket.class);
    private PacketList<UnderwritingInfoPacket> inPremiumPerPeriod = new PacketList<UnderwritingInfoPacket>(UnderwritingInfoPacket.class);
    private PacketList<AllPeriodUnderwritingInfoPacket> inAllPeriodUnderwritingInfo = new PacketList<AllPeriodUnderwritingInfoPacket>(AllPeriodUnderwritingInfoPacket.class);

    /**
     * Out information to be filled
     */
    private PacketList<ClaimCashflowPacket> outClaimsGross = new PacketList<ClaimCashflowPacket>(ClaimCashflowPacket.class);
    private PacketList<ClaimCashflowPacket> outClaimsNet = new PacketList<ClaimCashflowPacket>(ClaimCashflowPacket.class);
    private PacketList<ClaimCashflowPacket> outClaimsCeded = new PacketList<ClaimCashflowPacket>(ClaimCashflowPacket.class);
    private PacketList<ContractFinancialsPacket> outContractFinancials = new PacketList<ContractFinancialsPacket>(ContractFinancialsPacket.class);
    private PacketList<AdditionalPremium> outApAll = new PacketList<AdditionalPremium>(AdditionalPremium.class);
    private PacketList<PaidAdditionalPremium> outApAllPaid = new PacketList<PaidAdditionalPremium>(PaidAdditionalPremium.class);

    private IReinsuranceContractStrategy parmContractStructure = TemplateContractType.getDefault();

    /**
     * User parameters
     */
    private ContractCoverBase parmCoverageBase = ContractCoverBase.LOSSES_OCCURING;
    private ICoverStrategy parmCover = CoverStrategyType.getDefault();
    private ConstrainedMultiDimensionalParameter parmPremiumCover = new ConstrainedMultiDimensionalParameter(GroovyUtils.toList("[]"),
            Arrays.asList(PremiumSelectionConstraints.PREMIUM_TITLE), ConstraintsFactory.getConstraints(PremiumSelectionConstraints.IDENTIFIER));

    private IExposureBaseStrategy parmContractBase = ExposureBaseType.getDefault();

    /**
     * MAGIC STRINGS
     */

    public static final String GROSS_CLAIMS = "All covered cashflows";
    public static final String TERM_CALC= "paid term calculation cache";
    public static final String CEDED_CLAIMS = "All ceded cashflows";
    public static final String CEDED_INCURRED_CLAIMS = "All ceded cashflows";
    public static final String UWINFO = "All period UW info";
    private static final String INCOMING_PREMIUM = "Incoming premium";

    @Override
    protected void doCalculation() {
        initSimulation();
        initIteration();
        doFilter();
        storePremium();
        periodStore.put(CEDED_CLAIMS, new ArrayList<ClaimCashflowPacket>()); /* Otherwise null pointer on startup - overwrite this list later in this method */
        IAllContractClaimCache claimStore = (IAllContractClaimCache) periodStore.get(GROSS_CLAIMS, -periodScope.getCurrentPeriod());
        IPaidCalculation paidCalculation = (IPaidCalculation) periodStore.get(TERM_CALC, -periodScope.getCurrentPeriod());
        claimStore.cacheClaims(inClaims, periodScope.getCurrentPeriod());
        List<ClaimCashflowPacket> cededCashflowsToDate = allClaimsToDate(periodScope, periodStore, CEDED_CLAIMS);
        Set<ICededRoot> allIncurredCeded = RIUtilities.incurredCededClaims(cededCashflowsToDate, IncurredClaimBase.BASE);
        List<ICededRoot> allIncurredCededClaims = new ArrayList<ICededRoot>();
        allIncurredCededClaims.addAll(new ArrayList<ICededRoot>(allIncurredCeded));
        Map<Integer, Double> premiumPerPeriod = (Map<Integer, Double>) periodStore.get(INCOMING_PREMIUM, -periodScope.getCurrentPeriod());

        Double termLimit = parmContractStructure.getTermLimit();
        Double termExcess = parmContractStructure.getTermDeductible();

        LossAfterTermStructure incurredInPeriod = new TermIncurredCalculation().cededIncurredRespectTerm(claimStore, setupLayerParameters(),
                periodScope, termExcess, termLimit, parmCoverageBase, premiumPerPeriod);
        IncurredLossAndAP incurredLossAndAP = incurredInPeriod.getLossesByPeriod().get(periodScope.getCurrentPeriod());

        final List<ClaimCashflowPacket> paidClaims;
        final List<ContractFinancialsPacket> contractFinancialsPacket;
        try {
//            Leave this code here for the moment as evaulating the right hand side in the debugger can be useful, but it incurrs a performance penalty when uncommented.
//            final Map<Integer, Double> cededIncurredByPeriod = new TermIncurredCalculation().cededIncurredsByPeriods(incurredClaims.keys(), periodScope, termExcess, termLimit, setupLayerParameters() , parmCoverageBase);
            final List<ICededRoot> cededClaims = new IncurredAllocation().allocateClaims(incurredInPeriod.getLossAfterTermStructure(), claimStore, periodScope, parmCoverageBase);
            allIncurredCededClaims.addAll(cededClaims);

            final TermLossAndPaidAps paidStuff = paidCalculation.cededIncrementalPaidRespectTerm(claimStore, setupLayerParameters(),
                    periodScope, parmCoverageBase, termLimit, termExcess, globalSanityChecks, incurredInPeriod.getLossesByPeriod(), premiumPerPeriod);
            fillAPChannels(incurredLossAndAP, paidStuff);
            paidClaims =  new ProportionalToGrossPaidAllocation().allocatePaid(paidStuff.getTermLosses(), inClaims,
                    cededCashflowsToDate, periodScope, parmCoverageBase, allIncurredCededClaims, globalSanityChecks);
            contractFinancialsPacket = ContractFinancialsPacket.getContractFinancialsPacketsByInceptionPeriod(
                    paidClaims, new ArrayList<ClaimCashflowPacket>(), new ArrayList<CededUnderwritingInfoPacket>(),
                    new ArrayList<UnderwritingInfoPacket>(), periodScope.getPeriodCounter());
        } catch (SimulationException ex) {
            throw new SimulationException(" Insanity detected in structure module : " + this.getName() + "\n In iteration :"
                    + iterationScope.getCurrentIteration() + "\n Period : " + periodScope.getCurrentPeriod() + ". Seed : " + simulationScope.getSimulation().getRandomSeed() + "   .... Please see logs.   \n" + ex.getMessage(), ex);
        }
        RIUtilities.addMarkers(paidClaims, this);
        outClaimsCeded.addAll(paidClaims);
        outContractFinancials.addAll(contractFinancialsPacket);
        periodStore.put(CEDED_CLAIMS, paidClaims);

    }

    private void storePremium() {
        double subjectPremium = 0d;
        Map<Integer, Double> premiumPerPeriod = (Map<Integer, Double>) periodStore.get(INCOMING_PREMIUM, -periodScope.getCurrentPeriod());
        for (UnderwritingInfoPacket underwritingInfoPacket : inPremiumPerPeriod) {
            subjectPremium += underwritingInfoPacket.getPremiumWritten();
        }
        premiumPerPeriod.put(periodScope.getCurrentPeriod(), subjectPremium);
    }

    private void fillAPChannels(IncurredLossAndAP lossAndAP, TermLossAndPaidAps paidStuff) {
        outApAll.addAll(lossAndAP.getAddtionalPremiums());
        outApAllPaid.addAll(paidStuff.getPaidAPs());
    }

    private ScaledPeriodLayerParameters setupLayerParameters() {
        AllPeriodUnderwritingInfoPacket packet = (AllPeriodUnderwritingInfoPacket) iterationStore.get(UWINFO, -periodScope.getCurrentPeriod());
        ScaledPeriodLayerParameters layerParameters = ((NonPropTemplateContractStrategy) parmContractStructure).scalablePeriodLayerParameters();
        layerParameters.setExposureBase(parmContractBase.exposureBase());
        layerParameters.setCounter(periodScope.getPeriodCounter());
        layerParameters.setUwInfo(packet);
        return layerParameters;
    }

    private void initSimulation() {
        if (iterationScope.isFirstIteration() && periodScope.isFirstPeriod()) {
            if (parmContractBase.exposureBase() != ExposureBase.ABSOLUTE) {
                AllPeriodUnderwritingInfoPacket uwInfo = inAllPeriodUnderwritingInfo.get(0);
                iterationStore.put(UWINFO, uwInfo);
            } else {
                AllPeriodUnderwritingInfoPacket uwInfo = new AllPeriodUnderwritingInfoPacket();
                iterationStore.put(UWINFO, uwInfo);

            }
        }
    }

    private void initIteration(){
        if (periodScope.isFirstPeriod()) {
            final IAllContractClaimCache claimStore = new UberCacheClaimStore();
            final IPaidCalculation termCalc = new TermPaidRespectIncurredByClaim();
            periodStore.put(GROSS_CLAIMS, claimStore);
            periodStore.put(TERM_CALC, termCalc);
            periodStore.put(INCOMING_PREMIUM, new HashMap<Integer, Double >());
        }
    }

/*    private IncurredClaimAndAP incurredResultsThisSimPeriod(IAllContractClaimCache claimCache, double termLimit, double termExcess) {

        IIncurredCalculation incurredCalc = new TermIncurredCalculation();

        ScaledPeriodLayerParameters layerParameters = setupLayerParameters();
        List<LayerParameters> layers = layerParameters.getLayers(periodScope.getCurrentPeriod());
        List<IClaimRoot> incurredClaims = new ArrayList<IClaimRoot>(claimCache.allIncurredClaims());

        double incurredInPeriod = incurredCalc.cededIncurredRespectTerm(claimCache, layerParameters, periodScope, termExcess, termLimit, periodScope.getPeriodCounter(), parmCoverageBase);
        double additionalPremiumInperiod = incurredCalc.additionalPremiumAllLayers(
                claimCache.allIncurredClaimsCurrentModelPeriod(periodScope, parmCoverageBase), layers, 0d);
        return new IncurredClaimAndAP(incurredInPeriod, additionalPremiumInperiod);
    }*/

/*     private Map<Integer, Double> incrementalPaidAmountByModelPeriod(double termLimit, double termExcess, IAllContractClaimCache claimCache) {
        ScaledPeriodLayerParameters layerParameters = setupLayerParameters();

        TermPaidRespectIncurredByClaim iPaidCalculation = new TermPaidRespectIncurredByClaim();
        DateTime endPriorPeriod = periodScope.getCurrentPeriodStartDate().minusMillis(1);
        Map<Integer, Double> paidByModelPeriod = iPaidCalculation.cededIncrementalPaidRespectTerm(claimCache, layerParameters,
                periodScope, parmCoverageBase, termLimit, termExcess, globalSanityChecks);
        return paidByModelPeriod;
    }*/


    List<ClaimCashflowPacket> allClaimsToDate(PeriodScope periodScope, PeriodStore periodStore, String storeKey) {
        List<ClaimCashflowPacket> allClaims = new ArrayList<ClaimCashflowPacket>();
        for (int i = 0; i <= periodScope.getCurrentPeriod(); i++) {
            List<ClaimCashflowPacket> tempClaims = (List<ClaimCashflowPacket>) periodStore.get(storeKey, -i);
            allClaims.addAll(tempClaims);
        }
        return allClaims;
    }

    private void doFilter() {
        DateTime coverStart = periodScope.getPeriodCounter().startOfFirstPeriod();
        DateTime coverEnd = globalCover.getEndCover();

        List<ClaimCashflowPacket> uncoveredClaims = RIUtilities.uncoveredClaims(parmCoverageBase, coverStart, coverEnd, inClaims);

        inClaims.removeAll(uncoveredClaims);
        parmCover.coveredClaims(inClaims);
        outClaimsGross.addAll(inClaims);
        parmContractBase.coveredUnderwritingInfo(inUnderwritingInfo);
        filterPremium();
    }

    public void filterPremium() {
        List<IPremiumInfoMarker> coveredPremiums = (List<IPremiumInfoMarker>) parmPremiumCover.getValuesAsObjects(PremiumSelectionConstraints.PREMIUM_INDEX);
        List<UnderwritingInfoPacket> uncoveredPremium = Lists.newArrayList();
        for (UnderwritingInfoPacket underwritingInfoPacket : inPremiumPerPeriod) {
            for (IPremiumInfoMarker coveredPremium : coveredPremiums) {
                if(!underwritingInfoPacket.getOrigin().getName().equals(coveredPremium.getName())) {
                    uncoveredPremium.add(underwritingInfoPacket);
                }
            }
        }
        inPremiumPerPeriod.removeAll(uncoveredPremium);
    }

    public boolean isProportionalContract() {
        return false;
    }

    public IterationScope getIterationScope() {
        return iterationScope;
    }

    public void setIterationScope(IterationScope iterationScope) {
        this.iterationScope = iterationScope;
    }

    public PeriodStore getPeriodStore() {
        return periodStore;
    }

    public void setPeriodStore(PeriodStore periodStore) {
        this.periodStore = periodStore;
    }

    public PeriodScope getPeriodScope() {
        return periodScope;
    }

    public void setPeriodScope(PeriodScope periodScope) {
        this.periodScope = periodScope;
    }

    public PacketList<ClaimCashflowPacket> getInClaims() {
        return inClaims;
    }

    public void setInClaims(PacketList<ClaimCashflowPacket> inClaims) {
        this.inClaims = inClaims;
    }

    public PacketList<UnderwritingInfoPacket> getInUnderwritingInfo() {
        return inUnderwritingInfo;
    }

    public void setInUnderwritingInfo(PacketList<UnderwritingInfoPacket> inUnderwritingInfo) {
        this.inUnderwritingInfo = inUnderwritingInfo;
    }

    public PacketList<ClaimCashflowPacket> getOutClaimsGross() {
        return outClaimsGross;
    }

    public void setOutClaimsGross(PacketList<ClaimCashflowPacket> outClaimsGross) {
        this.outClaimsGross = outClaimsGross;
    }

    public PacketList<ClaimCashflowPacket> getOutClaimsNet() {
        return outClaimsNet;
    }

    public void setOutClaimsNet(PacketList<ClaimCashflowPacket> outClaimsNet) {
        this.outClaimsNet = outClaimsNet;
    }

    public PacketList<ClaimCashflowPacket> getOutClaimsCeded() {
        return outClaimsCeded;
    }

    public void setOutClaimsCeded(PacketList<ClaimCashflowPacket> outClaimsCeded) {
        this.outClaimsCeded = outClaimsCeded;
    }

    public PacketList<ContractFinancialsPacket> getOutContractFinancials() {
        return outContractFinancials;
    }

    public void setOutContractFinancials(PacketList<ContractFinancialsPacket> outContractFinancials) {
        this.outContractFinancials = outContractFinancials;
    }

    public ContractCoverBase getParmCoverageBase() {
        return parmCoverageBase;
    }

    public void setParmCoverageBase(ContractCoverBase parmCoverageBase) {
        this.parmCoverageBase = parmCoverageBase;
    }

    public ICoverStrategy getParmCover() {
        return parmCover;
    }

    public void setParmCover(ICoverStrategy parmCover) {
        this.parmCover = parmCover;
    }

    public IExposureBaseStrategy getParmContractBase() {
        return parmContractBase;
    }

    public void setParmContractBase(IExposureBaseStrategy parmContractBase) {
        this.parmContractBase = parmContractBase;
    }

    public IPeriodStrategy getGlobalCover() {
        return globalCover;
    }

    public void setGlobalCover(IPeriodStrategy globalCover) {
        this.globalCover = globalCover;
    }

    public boolean isGlobalSanityChecks() {
        return globalSanityChecks;
    }

    public void setGlobalSanityChecks(boolean globalSanityChecks) {
        this.globalSanityChecks = globalSanityChecks;
    }

    public IReinsuranceContractStrategy getParmContractStructure() {
        return parmContractStructure;
    }

    public void setParmContractStructure(IReinsuranceContractStrategy parmContractStructure) {
        this.parmContractStructure = parmContractStructure;
    }

    public PacketList<AllPeriodUnderwritingInfoPacket> getInAllPeriodUnderwritingInfo() {
        return inAllPeriodUnderwritingInfo;
    }

    public void setInAllPeriodUnderwritingInfo(PacketList<AllPeriodUnderwritingInfoPacket> inAllPeriodUnderwritingInfo) {
        this.inAllPeriodUnderwritingInfo = inAllPeriodUnderwritingInfo;
    }

    public IterationStore getIterationStore() {
        return iterationStore;
    }

    public void setIterationStore(IterationStore iterationStore) {
        this.iterationStore = iterationStore;
    }

    public SimulationScope getSimulationScope() {
        return simulationScope;
    }

    public void setSimulationScope(SimulationScope simulationScope) {
        this.simulationScope = simulationScope;
    }

   public ConstrainedMultiDimensionalParameter getParmPremiumCover() {
        return parmPremiumCover;
    }

    public void setParmPremiumCover(ConstrainedMultiDimensionalParameter parmPremiumCover) {
        this.parmPremiumCover = parmPremiumCover;
    }

    public PacketList<UnderwritingInfoPacket> getInPremiumPerPeriod() {
        return inPremiumPerPeriod;
    }

    public void setInPremiumPerPeriod(PacketList<UnderwritingInfoPacket> inPremiumPerPeriod) {
        this.inPremiumPerPeriod = inPremiumPerPeriod;
    }

    public PacketList<AdditionalPremium> getOutApAll() {
        return outApAll;
    }

    public void setOutApAll(PacketList<AdditionalPremium> outApAll) {
        this.outApAll = outApAll;
    }

    public PacketList<PaidAdditionalPremium> getOutApAllPaid() {
        return outApAllPaid;
    }

    public void setOutApAllPaid(PacketList<PaidAdditionalPremium> outApAllPaid) {
        this.outApAllPaid = outApAllPaid;
    }
}
