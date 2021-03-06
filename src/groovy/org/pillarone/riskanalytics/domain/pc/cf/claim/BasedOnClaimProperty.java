package org.pillarone.riskanalytics.domain.pc.cf.claim;

import org.pillarone.riskanalytics.domain.pc.cf.exposure.UnderwritingInfoPacket;

import java.util.Map;

/**
 * Used for commission and limit/deductible updating.
 *
 * @author stefan.kunz (at) intuitive-collaboration (dot) com
 */
public enum BasedOnClaimProperty {
    ULTIMATE_UNINDEXED {
        @Override
        public double premium(UnderwritingInfoPacket underwritingInfo) {
            return underwritingInfo.getPremiumWritten();
        }

        @Override
        public double incrementalIndexed(ClaimCashflowPacket claim) {
            return claim.ultimate();
        }

        @Override
        public double cumulatedIndexed(ClaimCashflowPacket claim) {
            return claim.developedUltimate();
        }
    },
    ULTIMATE_INDEXED {
        @Override
        public double incrementalIndexed(ClaimCashflowPacket claim) {
            return claim.totalIncrementalIndexed();
        }

        @Override
        public double cumulatedIndexed(ClaimCashflowPacket claim) {
            return claim.totalCumulatedIndexed();
        }

        @Override
        public double premium(UnderwritingInfoPacket underwritingInfo) {
            return underwritingInfo.getPremiumWritten();
        }
    },
    REPORTED {
        @Override
        public double premium(UnderwritingInfoPacket underwritingInfo) {
            return underwritingInfo.getPremiumWritten();
        }

        @Override
        public double incrementalIndexed(ClaimCashflowPacket claim) {
            return claim.getReportedIncrementalIndexed();
        }

        @Override
        public double cumulatedIndexed(ClaimCashflowPacket claim) {
            return claim.getReportedCumulatedIndexed();
        }
    }, PAID {
        @Override
        public double premium(UnderwritingInfoPacket underwritingInfo) {
            return underwritingInfo.getPremiumPaid();
        }

        @Override
        public double incrementalIndexed(ClaimCashflowPacket claim) {
            return claim.getPaidIncrementalIndexed();
        }

        @Override
        public double cumulatedIndexed(ClaimCashflowPacket claim) {
            return claim.getPaidCumulatedIndexed();
        }
    };

    public Object getConstructionString(Map parameters) {
        return getClass().getName() + "." + this;
    }

    public abstract double incrementalIndexed(ClaimCashflowPacket claim);
    public abstract double cumulatedIndexed(ClaimCashflowPacket claim);
    public abstract double premium(UnderwritingInfoPacket underwritingInfo);
}
