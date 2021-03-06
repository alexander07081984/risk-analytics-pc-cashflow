package org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.caching;

import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.pillarone.riskanalytics.core.simulation.SimulationException;
import org.pillarone.riskanalytics.core.simulation.engine.PeriodScope;
import org.pillarone.riskanalytics.domain.pc.cf.claim.ClaimCashflowPacket;
import org.pillarone.riskanalytics.domain.pc.cf.claim.ICededRoot;
import org.pillarone.riskanalytics.domain.pc.cf.claim.IClaimRoot;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.ClaimRIOutcome;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.ContractCoverBase;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.IncurredClaimRIOutcome;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.filterUtilities.GRIUtilities;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.filterUtilities.RIUtilities;

import java.util.*;

/**
 * author simon.parten @ art-allianz . com
 */
public class ContractClaimStoreTestIncurredClaimImpl implements IAllContractClaimCache {

    Set<IClaimRoot> someGrossClaims = Sets.newHashSet();
    Collection<ClaimCashflowPacket> claimCashflowPackets = Lists.newArrayList();

    public ContractClaimStoreTestIncurredClaimImpl(Collection<IClaimRoot> someClaims) {
        this.someGrossClaims = new HashSet<IClaimRoot>(someClaims);
    }

    public ContractClaimStoreTestIncurredClaimImpl(Set<IClaimRoot> someGrossClaims, Collection<ClaimCashflowPacket> claimCashflowPackets) {
        this.someGrossClaims = someGrossClaims;
        this.claimCashflowPackets = claimCashflowPackets;
    }

    public Collection<ClaimCashflowPacket> allClaimCashflowPackets() {
        return claimCashflowPackets;
    }

    public Set<IClaimRoot> allIncurredClaims() {
        if(someGrossClaims.size() == 0) {
            throw new SimulationException("misused");
        }
        return someGrossClaims;
    }

    public SetMultimap<IClaimRoot, IClaimRoot> incurredClaimsByKey() {
        throw new SimulationException("");
    }

    public void cacheClaims(Collection<ClaimCashflowPacket> claims) {
        throw new SimulationException("");
    }

    public Collection<ClaimCashflowPacket> allClaimCashflowPacketsInModelPeriod(Integer uwPeriod, PeriodScope periodScope, ContractCoverBase base) {
        throw new SimulationException("");
    }

    public Set<IClaimRoot> allIncurredClaimsInModelPeriod(Integer anInt, PeriodScope periodScope, ContractCoverBase coverBase) {
        return RIUtilities.incurredClaimsByPeriod(anInt, periodScope.getPeriodCounter(), someGrossClaims, coverBase );
    }

    public Set<IClaimRoot> allIncurredClaimsCurrentModelPeriodForAllocation(PeriodScope periodScope, ContractCoverBase coverBase) {
        return someGrossClaims;
    }

    @Override
    public Collection<IClaimRoot> allIncurredClaimsUpToSimulationPeriod(Integer period, PeriodScope periodScope, ContractCoverBase coverBase) {
        throw new SimulationException("");
    }

    @Override
    public Collection<ClaimCashflowPacket> allCashflowClaimsUpToSimulationPeriod(Integer simulationPeriod, PeriodScope periodScope, ContractCoverBase coverBase) {
        throw new SimulationException("");
    }

    @Override
    public Collection<ClaimCashflowPacket> allClaimCashflowPacketsInSimulationPeriod(Integer anInt, PeriodScope periodScope, ContractCoverBase base) {
        throw new SimulationException("");
    }

    @Override
    public Collection<IClaimRoot> allIncurredClaimsInSimulationPeriod(Integer period, PeriodScope periodScope, ContractCoverBase coverBase) {
        throw new SimulationException("");
    }

    @Override
    public Collection<IClaimRoot> allIncurredClaimsCurrentSimulationPeriod(PeriodScope periodScope, ContractCoverBase coverBase) {
        return new ArrayList<IClaimRoot>();
    }

    @Override
    public void cacheClaims(Collection<ClaimCashflowPacket> newClaims, Integer simulationPeriod) {
        throw new SimulationException("");
    }

    public Collection<ClaimCashflowPacket> cashflowsByUnderwritingPeriodUpToSimulationPeriod(Integer simulationPeriod, Integer underwritingPeriod, PeriodScope periodScope, ContractCoverBase coverBase) {

        Collection<ClaimCashflowPacket> cashflowsPaidAgainsThisModelPeriod = GRIUtilities.cashflowsCoveredInModelPeriod(claimCashflowPackets, periodScope, coverBase, underwritingPeriod);
        return cashflowsPaidAgainsThisModelPeriod;
    }


    @Override
    public List<ClaimCashflowPacket> allCededCashlowsToDate() {
        throw new SimulationException("");
    }

    @Override
    public List<ICededRoot> allCededRootClaimsToDate() {
        throw new SimulationException("");
    }

    @Override
    public void cacheCededClaims(final List<ClaimRIOutcome> cededCashflows, final List<IncurredClaimRIOutcome> cededIncurred) {
        throw new SimulationException("");
    }

    @Override
    public List<ClaimRIOutcome> allRIOutcomesToDate() {
        throw new SimulationException("");
    }

    @Override
    public Collection<IncurredClaimRIOutcome> allIncurredRIOutcomesToDate() {
        throw new SimulationException("");
    }
}
