package org.pillarone.riskanalytics.domain.pc.cf.claim.generator.attritional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pillarone.riskanalytics.core.parameterization.ConstrainedMultiDimensionalParameter;
import org.pillarone.riskanalytics.core.parameterization.ConstrainedString;
import org.pillarone.riskanalytics.core.parameterization.ConstraintsFactory;
import org.pillarone.riskanalytics.core.simulation.IPeriodCounter;
import org.pillarone.riskanalytics.core.util.GroovyUtils;
import org.pillarone.riskanalytics.domain.pc.cf.accounting.experienceAccounting.CommutationState;
import org.pillarone.riskanalytics.domain.pc.cf.claim.ClaimCashflowPacket;
import org.pillarone.riskanalytics.domain.pc.cf.claim.ClaimRoot;
import org.pillarone.riskanalytics.domain.pc.cf.claim.ClaimType;
import org.pillarone.riskanalytics.domain.pc.cf.claim.generator.AbstractClaimsGenerator;
import org.pillarone.riskanalytics.domain.pc.cf.claim.generator.contractBase.IReinsuranceContractBaseStrategy;
import org.pillarone.riskanalytics.domain.pc.cf.claim.generator.contractBase.ReinsuranceContractBaseType;
import org.pillarone.riskanalytics.domain.pc.cf.indexing.BaseDateMode;
import org.pillarone.riskanalytics.domain.pc.cf.indexing.Factors;
import org.pillarone.riskanalytics.domain.pc.cf.indexing.IndexMode;
import org.pillarone.riskanalytics.domain.pc.cf.indexing.IndexUtils;
import org.pillarone.riskanalytics.domain.pc.cf.pattern.IPayoutPatternMarker;
import org.pillarone.riskanalytics.domain.pc.cf.reserve.updating.aggregate.AggregateActualClaimsStrategyType;
import org.pillarone.riskanalytics.domain.pc.cf.reserve.updating.aggregate.AggregateUpdatingMethodologyStrategyType;
import org.pillarone.riskanalytics.domain.pc.cf.reserve.updating.aggregate.IAggregateActualClaimsStrategy;
import org.pillarone.riskanalytics.domain.pc.cf.reserve.updating.aggregate.IAggregateUpdatingMethodologyStrategy;
import org.pillarone.riskanalytics.domain.utils.constraint.DoubleConstraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author stefan.kunz (at) intuitive-collaboration (dot) com
 */
public class AttritionalClaimsGenerator extends AbstractClaimsGenerator {

    static Log LOG = LogFactory.getLog(AttritionalClaimsGenerator.class);

    private AttritionalClaimsModel subClaimsModel = new AttritionalClaimsModel();
    private IReinsuranceContractBaseStrategy parmParameterizationBasis = ReinsuranceContractBaseType.getStrategy(
            ReinsuranceContractBaseType.PLEASESELECT, new HashMap());
    private ConstrainedString parmPayoutPattern = new ConstrainedString(IPayoutPatternMarker.class, "");
    private IAggregateActualClaimsStrategy parmActualClaims = AggregateActualClaimsStrategyType.getDefault();
    private IAggregateUpdatingMethodologyStrategy parmUpdatingMethodology = AggregateUpdatingMethodologyStrategyType.getDefault();
    private ConstrainedMultiDimensionalParameter parmDeterministicClaims = new ConstrainedMultiDimensionalParameter(
            GroovyUtils.convertToListOfList(new Object[]{0d, 0d}), Arrays.asList(REAL_PERIOD, CLAIM_VALUE),
            ConstraintsFactory.getConstraints(DoubleConstraints.IDENTIFIER));


    @Override
    protected void doCalculation(String phase) {
//        A deal may commute before the end of the contract period. We may hence want to terminate claims generation
//        Depending on the outcome in the experience account.

        if (phase.equals(PHASE_CLAIMS_CALCULATION)) {
            CommutationState commutationState = (CommutationState) (periodStore.get(COMMUTATION_STATE));

            if (commutationState.checkCommutation(periodScope)) {
                IPeriodCounter periodCounter = periodScope.getPeriodCounter();
                List<ClaimRoot> baseClaims;
                if (globalDeterministicMode) {
                    baseClaims = getDeterministicClaims(parmDeterministicClaims, periodScope, ClaimType.ATTRITIONAL);
                } else {
                    List<Factors> severityFactors = IndexUtils.filterFactors(inFactors, subClaimsModel.getParmSeverityIndices(),
                            IndexMode.STEPWISE_PREVIOUS, BaseDateMode.START_OF_PROJECTION, null);
                    baseClaims = subClaimsModel.baseClaims(inUnderwritingInfo, inEventFrequencies, inEventSeverities,
                            severityFactors, parmParameterizationBasis, this, periodScope);
                }
                baseClaims = parmUpdatingMethodology.updatingUltimate(baseClaims, parmActualClaims, periodCounter, globalUpdateDate, inPatterns);
                List<Factors> runoffFactors = new ArrayList<Factors>();
                List<ClaimCashflowPacket> claims = claimsOfCurrentPeriod(baseClaims, parmPayoutPattern, parmActualClaims,
                        periodScope, runoffFactors);
                developClaimsOfFormerPeriods(claims, periodCounter, runoffFactors);
                setTechnicalProperties(claims);
                outClaims.addAll(claims);
            }
        } else if (phase.equals(PHASE_STORE_COMMUTATION_STATE)) {
            if (inCommutationState != null && inCommutationState.size() == 1) {
                CommutationState packet = inCommutationState.get(0);
                periodStore.put(COMMUTATION_STATE, packet, 1);
            } else {
                throw new IllegalArgumentException("Found different to one commutation in inCommutationState");
            }
        } else {
            throw new RuntimeException("Unkown phase: " + phase);
        }
    }

    public void allocateChannelsToPhases() {
//          Calculation channels --------------------------------------------------------------------------
        setTransmitterPhaseInput(inPatterns, PHASE_CLAIMS_CALCULATION);
        setTransmitterPhaseInput(inEventSeverities, PHASE_CLAIMS_CALCULATION);
        setTransmitterPhaseInput(inEventFrequencies, PHASE_CLAIMS_CALCULATION);
        setTransmitterPhaseInput(inFactors, PHASE_CLAIMS_CALCULATION);
        setTransmitterPhaseInput(inUnderwritingInfo, PHASE_CLAIMS_CALCULATION);

        setTransmitterPhaseOutput(outClaims, PHASE_CLAIMS_CALCULATION);

//          Commutation channels --------------------------------------------------------------------------
        setTransmitterPhaseInput(inCommutationState, PHASE_STORE_COMMUTATION_STATE);
    }


    public AttritionalClaimsModel getSubClaimsModel() {
        return subClaimsModel;
    }

    public void setSubClaimsModel(AttritionalClaimsModel subClaimsModel) {
        this.subClaimsModel = subClaimsModel;
    }

    public IReinsuranceContractBaseStrategy getParmParameterizationBasis() {
        return parmParameterizationBasis;
    }

    public void setParmParameterizationBasis(IReinsuranceContractBaseStrategy parmParameterizationBasis) {
        this.parmParameterizationBasis = parmParameterizationBasis;
    }

    public ConstrainedString getParmPayoutPattern() {
        return parmPayoutPattern;
    }

    public void setParmPayoutPattern(ConstrainedString parmPayoutPattern) {
        this.parmPayoutPattern = parmPayoutPattern;
    }

    public ConstrainedMultiDimensionalParameter getParmDeterministicClaims() {
        return parmDeterministicClaims;
    }

    public void setParmDeterministicClaims(ConstrainedMultiDimensionalParameter parmDeterministicClaims) {
        this.parmDeterministicClaims = parmDeterministicClaims;
    }

    public IAggregateActualClaimsStrategy getParmActualClaims() {
        return parmActualClaims;
    }

    public void setParmActualClaims(IAggregateActualClaimsStrategy parmActualClaims) {
        this.parmActualClaims = parmActualClaims;
    }

    public IAggregateUpdatingMethodologyStrategy getParmUpdatingMethodology() {
        return parmUpdatingMethodology;
    }

    public void setParmUpdatingMethodology(IAggregateUpdatingMethodologyStrategy parmUpdatingMethodology) {
        this.parmUpdatingMethodology = parmUpdatingMethodology;
    }
}
