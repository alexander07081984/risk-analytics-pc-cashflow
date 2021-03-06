package org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.filterUtilities;

import com.google.common.collect.*;
import org.joda.time.DateTime;
import org.pillarone.riskanalytics.core.components.IComponentMarker;
import org.pillarone.riskanalytics.core.simulation.IPeriodCounter;
import org.pillarone.riskanalytics.core.simulation.SimulationException;
import org.pillarone.riskanalytics.domain.pc.cf.claim.*;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.ContractCoverBase;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.IncurredClaimBase;
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.IncurredClaimRIOutcome;

import java.util.*;

/**
 * author simon.parten @ art-allianz . com
 */
public class RIUtilities {

    public static Set<IClaimRoot> incurredClaims(Collection<ClaimCashflowPacket> allCashflows, IncurredClaimBase incurredClaimBase) {
        Set<IClaimRoot> iClaimRoots = new HashSet<IClaimRoot>();
        for (ClaimCashflowPacket aClaim : allCashflows) {
            iClaimRoots.add(incurredClaimBase.parentClaim(aClaim));
        }
        return iClaimRoots;
    }

    public static SetMultimap<IClaimRoot, IClaimRoot> incurredClaims(Collection<ClaimCashflowPacket> allCashflows) {
        SetMultimap<IClaimRoot, IClaimRoot> grossClaimsByKeyClaim = HashMultimap.create();

        for (ClaimCashflowPacket aClaim : allCashflows) {
            grossClaimsByKeyClaim.put(aClaim.getKeyClaim(), aClaim.getBaseClaim());
        }
        return grossClaimsByKeyClaim;
    }

    public static void  addMarkers(Collection<ClaimCashflowPacket> packets, IComponentMarker sender ) {
        for (ClaimCashflowPacket packet : packets) {
            packet.setMarker(sender);
        }

    }

    public static Set<ICededRoot> incurredCededClaims(Collection<ClaimCashflowPacket> allCashflows, IncurredClaimBase incurredClaimBase) {
        Set<ICededRoot> iClaimRoots = new HashSet<ICededRoot>();
        for (ClaimCashflowPacket aClaim : allCashflows) {
            iClaimRoots.add(incurredClaimBase.parentCededClaim(aClaim));
        }
        return iClaimRoots;
    }


    public static Set<IClaimRoot> incurredClaimsByDate(DateTime startDate, DateTime endDate, Collection<IClaimRoot> allIncurredClaims, ContractCoverBase coverBase) {
        Set<IClaimRoot> claimsOfInterest = new HashSet<IClaimRoot>();
        for (IClaimRoot anIncurredClaim : allIncurredClaims) {
            DateTime coverDateTime = coverBase.claimCoverDate(anIncurredClaim);
            if ((coverDateTime.equals(startDate) || coverDateTime.isAfter(startDate)) && coverDateTime.isBefore(endDate)) {
                claimsOfInterest.add(anIncurredClaim);
            }
        }
        return claimsOfInterest;
    }

    public static Set<IClaimRoot> incurredClaimsByDate(DateTime startDate, DateTime endDate, Multimap<IClaimRoot, IClaimRoot> allIncurredClaims, ContractCoverBase coverBase, IncurredClaimBase claimBase) {
        Set<IClaimRoot> claimsOfInterest = new HashSet<IClaimRoot>();
        Collection<IClaimRoot> incurredClaimsOfInterest;

        switch (claimBase) {
            case BASE:
                incurredClaimsOfInterest = allIncurredClaims.values();
                break;
            case KEY:
                incurredClaimsOfInterest = allIncurredClaims.keys();
                break;
            default:
                throw new SimulationException("Unknown claim base " + claimBase.toString() );
        }

        for (IClaimRoot anIncurredClaim : incurredClaimsOfInterest) {
            DateTime coverDateTime = coverBase.claimCoverDate(anIncurredClaim);
            if ((coverDateTime.equals(startDate) || coverDateTime.isAfter(startDate)) && coverDateTime.isBefore(endDate)) {
                claimsOfInterest.add(anIncurredClaim);
            }
        }
        return claimsOfInterest;
    }

    public static Set<IClaimRoot> incurredClaimsByPeriod(DateTime startDate, DateTime endDate, Multimap<IClaimRoot, IClaimRoot> allIncurredClaims, ContractCoverBase coverBase, IncurredClaimBase claimBase) {
        Set<IClaimRoot> claimsOfInterest = new HashSet<IClaimRoot>();
        Collection<IClaimRoot> incurredClaimsOfInterest;

        switch (claimBase) {
            case BASE:
                incurredClaimsOfInterest = allIncurredClaims.values();
                break;
            case KEY:
                incurredClaimsOfInterest = allIncurredClaims.keys();
                break;
            default:
                throw new SimulationException("Unknown claim base " + claimBase.toString() );
        }

        for (IClaimRoot anIncurredClaim : incurredClaimsOfInterest) {
            DateTime coverDateTime = coverBase.claimCoverDate(anIncurredClaim);
            if ((coverDateTime.equals(startDate) || coverDateTime.isAfter(startDate)) && coverDateTime.isBefore(endDate)) {
                claimsOfInterest.add(anIncurredClaim);
            }
        }
        return claimsOfInterest;
    }


    public static List<ClaimCashflowPacket> cashflowClaimsByOccurenceDate(DateTime startDate, DateTime endDate, List<ClaimCashflowPacket> cashflows) {
        List<ClaimCashflowPacket> claimsOfInterest = new ArrayList<ClaimCashflowPacket>();
        for (ClaimCashflowPacket aCashflow : cashflows) {
            DateTime coverDate = aCashflow.getDate();
            if ((coverDate.equals(startDate) || coverDate.isAfter(startDate)) && coverDate.isBefore(endDate)) {
                claimsOfInterest.add(aCashflow);
            }
        }
        return claimsOfInterest;
    }

    /**
     * This method attempts to find the lastest cashflow by incurred claim. It first attempts to find the first non-zero cashflow.
     *
     * If none is found it searches for a zero cashflow.
     *
     * @param cashflows
     * @param base
     * @return
     */
    public static Collection<ClaimCashflowPacket> latestCashflowByIncurredClaim(Collection<ClaimCashflowPacket> cashflows, IncurredClaimBase base) {
        Set<IClaimRoot> claimRoots = RIUtilities.incurredClaims(cashflows, base);

        ArrayListMultimap<IClaimRoot, ClaimCashflowPacket> cashflowsByKey = cashflowsByRoot(cashflows, base);
        List<ClaimCashflowPacket> latestUpdates = Lists.newArrayList();

        for (IClaimRoot claimRoot : claimRoots) {

            IClaimRoot claimRoot1 = new ClaimRoot(0, ClaimType.ATTRITIONAL, null, new DateTime(1900, 1, 1, 1, 1, 1, 1));
            ClaimCashflowPacket latestPacket = new ClaimCashflowPacket(claimRoot1, claimRoot1);

            List<ClaimCashflowPacket> cashflowPackets = cashflowsByKey.get(claimRoot);
            boolean foundNonZeroCashflow = false;
            for (ClaimCashflowPacket cashflowPacket : cashflowPackets) {
                if (cashflowPacket.getDate().isAfter(latestPacket.getDate()) && cashflowPacket.getPaidCumulatedIndexed() > 0) {
                    latestPacket = cashflowPacket;
                    foundNonZeroCashflow = true;
                }
            }
            if(foundNonZeroCashflow) {
                latestUpdates.add(latestPacket);
            } else {
                for (ClaimCashflowPacket cashflowPacket : cashflowPackets) {
                    if (cashflowPacket.getDate().isAfter(latestPacket.getDate())) {
                        latestPacket = cashflowPacket;
                    }
                }
                latestUpdates.add(latestPacket);
            }
        }

        return latestUpdates;


    }

    public static double incrementalPaidSum(Collection<ClaimCashflowPacket> packets) {
        return GRIUtilities.incrementalCashflowSum(packets);
    }

    public static double ultimateSumFromCashflows(Collection<ClaimCashflowPacket> packets) {
        Set<IClaimRoot> incurredClaims = RIUtilities.incurredClaims(packets, IncurredClaimBase.BASE);
        ArrayList<IClaimRoot> claims = new ArrayList<IClaimRoot>();
        claims.addAll(incurredClaims);
        return ultimateSum(incurredClaims);
    }

    public static Double ultimateSum(Collection<IClaimRoot> incurredClaims) {
        if (incurredClaims.size() > 0) {
            double sum = 0d;
            for (IClaimRoot incurredClaim : incurredClaims) {
                sum += incurredClaim.getUltimate();
            }
            return sum;
        }
        return 0d;
    }

    public static Double incrementalCashflowSum(Collection<ClaimCashflowPacket> incurredClaims) {
        if (incurredClaims.size() > 0) {
            double sum = 0d;
            for (ClaimCashflowPacket incurredClaim : incurredClaims) {
                sum += incurredClaim.getPaidIncrementalIndexed();
            }
            return sum;
        }
        return 0d;
    }


    public static ArrayListMultimap<IClaimRoot, ClaimCashflowPacket> cashflowsByRoot(Collection<ClaimCashflowPacket> cashflows, IncurredClaimBase base) {
        ArrayListMultimap<IClaimRoot, ClaimCashflowPacket> cashflowsByKey = ArrayListMultimap.create();
        for (ClaimCashflowPacket aCashflow : cashflows) {
            cashflowsByKey.put(base.parentClaim(aCashflow), aCashflow);
        }
        return cashflowsByKey;
    }

    public static List<ClaimCashflowPacket> cashflowsByIncurredDate(DateTime startDate, DateTime endDate, Collection<ClaimCashflowPacket> cashflows, ContractCoverBase coverBase) {
        List<ClaimCashflowPacket> claimsOfInterest = new ArrayList<ClaimCashflowPacket>();
        for (ClaimCashflowPacket anIncurredClaim : cashflows) {
            DateTime coverDateTime = coverBase.claimCoverDate(anIncurredClaim);
            if ((coverDateTime.equals(startDate) || coverDateTime.isAfter(startDate)) && coverDateTime.isBefore(endDate)) {
                claimsOfInterest.add(anIncurredClaim);
            }
        }
        return claimsOfInterest;
    }


    public static Set<IClaimRoot> incurredClaimsByPeriod(Integer period, IPeriodCounter periodCounter, Collection<IClaimRoot> allIncurredClaims, ContractCoverBase coverBase) {
        Set<IClaimRoot> claimsOfInterest = new HashSet<IClaimRoot>();
        for (IClaimRoot anIncurredClaim : allIncurredClaims) {
            DateTime coverDateTime = coverBase.claimCoverDate(anIncurredClaim);
            if (periodCounter.belongsToPeriod(coverDateTime) == period) {
                claimsOfInterest.add(anIncurredClaim);
            }
        }
        return claimsOfInterest;
    }

    public static List<ClaimCashflowPacket> cashflowsClaimsByPeriod(Integer period, IPeriodCounter periodCounter, Collection<ClaimCashflowPacket> cashflowsClaims, ContractCoverBase coverBase) {
        List<ClaimCashflowPacket> claimsOfInterest = new ArrayList<ClaimCashflowPacket>();
        for (ClaimCashflowPacket aClaim : cashflowsClaims) {
            DateTime coverDateTime = coverBase.claimCoverDate(aClaim);
            if (period == periodCounter.belongsToPeriod(coverDateTime)) {
                claimsOfInterest.add(aClaim);
            }
        }
        return claimsOfInterest;
    }

    public static List<ClaimCashflowPacket> uncoveredClaims(ContractCoverBase coverageBase, DateTime coverStart, DateTime coverEnd, List<ClaimCashflowPacket> incomingClaims) {
        final List<ClaimCashflowPacket> uncoveredClaims = new ArrayList<ClaimCashflowPacket>();

        for (ClaimCashflowPacket incomingClaim : incomingClaims) {
            DateTime claimCoverDate = coverageBase.claimCoverDate(incomingClaim);
            boolean inCoverPeriod = (coverStart.isEqual(claimCoverDate) || coverStart.isBefore(claimCoverDate)) && coverEnd.isAfter(claimCoverDate);
            if(!inCoverPeriod) {
                uncoveredClaims.add(incomingClaim);
            }
        }
        return uncoveredClaims;
    }

    public static IncurredClaimRIOutcome findCededClaimRelatedToGrossClaim(IClaimRoot grossClaim, Collection<IncurredClaimRIOutcome> incurredCededClaims) {
        for (IncurredClaimRIOutcome incurredCededClaim : incurredCededClaims) {
            if(grossClaim.equals(incurredCededClaim.getGrossClaim())) {
                return incurredCededClaim;
            }
            if(incurredCededClaim.getGrossClaim() instanceof ICededRoot) {
                if(((ICededRoot) incurredCededClaim.getGrossClaim()).getGrossClaim().equals(grossClaim)) {
                    return incurredCededClaim;
                }
            }
        }
        throw new SimulationException("Failed to match a gross claim to the list of ceded claims; " + grossClaim.toString());
    }

    public static ClaimCashflowPacket findCashflowToGrossClaim(IClaimRoot cededKeyClaim, Collection<ClaimCashflowPacket> allCashflows, IncurredClaimBase base) {
        for (ClaimCashflowPacket allCashflow : allCashflows) {
            if (cededKeyClaim.equals(base.parentClaim(allCashflow))) {
                return allCashflow;
            }
        }
        return new ClaimCashflowPacket();
    }

}
