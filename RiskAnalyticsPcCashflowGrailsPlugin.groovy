import org.pillarone.riskanalytics.core.output.DrillDownMode
import org.pillarone.riskanalytics.domain.pc.cf.output.SplitAndFilterCollectionModeStrategy
import org.pillarone.riskanalytics.domain.pc.cf.pattern.PatternTableConstraints
import org.pillarone.riskanalytics.core.parameterization.ConstraintsFactory
import org.pillarone.riskanalytics.domain.pc.cf.indexing.AnnualIndexTableConstraints
import org.pillarone.riskanalytics.domain.pc.cf.indexing.SeverityIndexSelectionTableConstraints
import org.pillarone.riskanalytics.domain.pc.cf.indexing.FrequencyIndexSelectionTableConstraints
import org.pillarone.riskanalytics.domain.pc.cf.indexing.PremiumIndexSelectionTableConstraints
import org.pillarone.riskanalytics.domain.pc.cf.indexing.PolicyIndexSelectionTableConstraints
import org.pillarone.riskanalytics.domain.pc.cf.legalentity.LegalEntityPortionConstraints
import org.pillarone.riskanalytics.domain.pc.cf.indexing.DeterministicIndexTableConstraints

import org.pillarone.riskanalytics.domain.pc.cf.claim.generator.validation.PMLClaimsGeneratorStrategyValidator
import org.pillarone.riskanalytics.core.parameterization.validation.ValidatorRegistry
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.ContractFinancialsPacket
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.proportional.commission.param.InterpolatedSlidingCommissionValidator
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.proportional.lossparticipation.LossParticipationValidator
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.cover.CoverMap
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.cover.MatrixStructureContraints
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.cover.validation.CoverAttributeValidator
import org.pillarone.riskanalytics.domain.pc.cf.segment.FinancialsPacket as FP
import org.pillarone.riskanalytics.domain.utils.constraint.DoubleConstraints
import org.pillarone.riskanalytics.domain.utils.constraint.DateTimeConstraints
import org.pillarone.riskanalytics.core.util.ResourceBundleRegistry
import org.pillarone.riskanalytics.domain.pc.cf.pattern.validation.RecoveryPatternStrategyValidator
import org.pillarone.riskanalytics.domain.pc.cf.pattern.validation.PatternStrategyValidator
import org.pillarone.riskanalytics.domain.pc.cf.indexing.LinkRatioIndexTableConstraints
import org.pillarone.riskanalytics.domain.pc.cf.indexing.ReservesIndexSelectionTableConstraints
import org.pillarone.riskanalytics.domain.pc.cf.claim.generator.validation.ClaimsGeneratorScalingValidator
import org.pillarone.riskanalytics.domain.pc.cf.claim.allocation.validation.RiskAllocationValidator
import org.pillarone.riskanalytics.domain.pc.cf.claim.ClaimTypeSelectionTableConstraints
import org.pillarone.riskanalytics.domain.pc.cf.dependency.validation.CopulaValidator
import org.pillarone.riskanalytics.domain.pc.cf.dependency.validation.MultipleProbabilitiesCopulaValidator
import org.pillarone.riskanalytics.core.output.aggregation.PacketAggregatorRegistry
import org.pillarone.riskanalytics.domain.pc.cf.claim.ClaimCashflowPacket as CCP
import org.pillarone.riskanalytics.domain.pc.cf.claim.ClaimPacketAggregator
import org.pillarone.riskanalytics.domain.pc.cf.discounting.YieldCurveTableConstraints
import org.pillarone.riskanalytics.domain.pc.cf.output.AggregateSplitPerSourceCollectingModeStrategy
import org.pillarone.riskanalytics.core.output.CollectingModeFactory
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.validation.XLStrategyValidator
import org.pillarone.riskanalytics.domain.pc.cf.exposure.UnderwritingInfoPacket
import org.pillarone.riskanalytics.domain.pc.cf.exposure.UnderwritingInfoPacketAggregator
import org.pillarone.riskanalytics.domain.utils.constraint.ReinsuranceContractBasedOn
import org.pillarone.riskanalytics.domain.utils.constraint.ReinsuranceContractContraints
import org.pillarone.riskanalytics.domain.utils.constraint.SegmentPortion
import org.pillarone.riskanalytics.domain.pc.cf.output.AggregateUltimateClaimCollectingModeStrategy
import org.pillarone.riskanalytics.domain.pc.cf.output.AggregateUltimateReportedPaidClaimCollectingModeStrategy
import org.pillarone.riskanalytics.domain.pc.cf.output.AggregateUltimatePaidClaimCollectingModeStrategy
import org.pillarone.riskanalytics.domain.pc.cf.structure.validation.ClaimTypeStructuringValidator
import org.pillarone.riskanalytics.domain.pc.cf.output.AggregateSplitPerSourceReducedCollectingModeStrategy
import org.pillarone.riskanalytics.domain.pc.cf.output.AggregateUltimateReportedClaimCollectingModeStrategy
import org.pillarone.riskanalytics.domain.pc.cf.indexing.RunOffIndexSelectionTableConstraints
import org.pillarone.riskanalytics.domain.pc.cf.output.SingleUltimatePaidClaimCollectingModeStrategy
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.constraints.PremiumSelectionConstraints
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.additionalPremium.AdditionalPremium
import org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.stateless.additionalPremium.PaidAdditionalPremium

class RiskAnalyticsPcCashflowGrailsPlugin {
    // the plugin version
    def version = "0.6.16-kti"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.7 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Intuitive Collaboration AG"
    def authorEmail = "info (at) intuitive-collaboration (dot) com"
    def title = "Property Casualty Library for Cashflow Models"
    def description = '''\\
'''

    // URL to the plugin's documentation
    def documentation = "http://www.pillarone.org"

    def groupId = "org.pillarone"


    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before 
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        ConstraintsFactory.registerConstraint(new AnnualIndexTableConstraints())
        ConstraintsFactory.registerConstraint(new LinkRatioIndexTableConstraints())
        ConstraintsFactory.registerConstraint(new DeterministicIndexTableConstraints())
        ConstraintsFactory.registerConstraint(new PolicyIndexSelectionTableConstraints())
        ConstraintsFactory.registerConstraint(new PremiumIndexSelectionTableConstraints())
        ConstraintsFactory.registerConstraint(new FrequencyIndexSelectionTableConstraints())
        ConstraintsFactory.registerConstraint(new SeverityIndexSelectionTableConstraints())
        ConstraintsFactory.registerConstraint(new RunOffIndexSelectionTableConstraints())
        ConstraintsFactory.registerConstraint(new ReservesIndexSelectionTableConstraints())
        ConstraintsFactory.registerConstraint(new LegalEntityPortionConstraints())
        ConstraintsFactory.registerConstraint(new PatternTableConstraints())
        ConstraintsFactory.registerConstraint(new CoverMap())
        ConstraintsFactory.registerConstraint(new MatrixStructureContraints())
        ConstraintsFactory.registerConstraint(new DoubleConstraints())
        ConstraintsFactory.registerConstraint(new DateTimeConstraints())
        ConstraintsFactory.registerConstraint(new ClaimTypeSelectionTableConstraints())
        ConstraintsFactory.registerConstraint(new YieldCurveTableConstraints())
        ConstraintsFactory.registerConstraint(new SegmentPortion())
        ConstraintsFactory.registerConstraint(new ReinsuranceContractContraints())
        ConstraintsFactory.registerConstraint(new PremiumSelectionConstraints())
        ConstraintsFactory.registerConstraint(new ReinsuranceContractBasedOn())

        ValidatorRegistry.addValidator(new PMLClaimsGeneratorStrategyValidator())
        ValidatorRegistry.addValidator(new PatternStrategyValidator())
        ValidatorRegistry.addValidator(new RecoveryPatternStrategyValidator())
        ValidatorRegistry.addValidator(new ClaimsGeneratorScalingValidator())
        ValidatorRegistry.addValidator(new RiskAllocationValidator())
        ValidatorRegistry.addValidator(new CopulaValidator())
        ValidatorRegistry.addValidator(new MultipleProbabilitiesCopulaValidator())
        ValidatorRegistry.addValidator(new XLStrategyValidator())
        ValidatorRegistry.addValidator(new ClaimTypeStructuringValidator())
        ValidatorRegistry.addValidator(new CoverAttributeValidator())
        ValidatorRegistry.addValidator(new LossParticipationValidator())
        ValidatorRegistry.addValidator(new InterpolatedSlidingCommissionValidator())

        ResourceBundleRegistry.addBundle(ResourceBundleRegistry.VALIDATION, "org.pillarone.riskanalytics.domain.pc.cf.claim.generator.validation.pMLClaimsGeneratorStrategyValidator")
        ResourceBundleRegistry.addBundle(ResourceBundleRegistry.VALIDATION, "org.pillarone.riskanalytics.domain.pc.cf.pattern.validation.patternStrategyValidator")
        ResourceBundleRegistry.addBundle(ResourceBundleRegistry.VALIDATION, "org.pillarone.riskanalytics.domain.pc.cf.pattern.validation.recoveryPatternStrategyValidator")
        ResourceBundleRegistry.addBundle(ResourceBundleRegistry.VALIDATION, "org.pillarone.riskanalytics.domain.pc.cf.claim.generator.validation.claimsGeneratorScalingValidator")
        ResourceBundleRegistry.addBundle(ResourceBundleRegistry.VALIDATION, "org.pillarone.riskanalytics.domain.pc.cf.claim.allocation.validation.riskAllocationValidator")
        ResourceBundleRegistry.addBundle(ResourceBundleRegistry.VALIDATION, "org.pillarone.riskanalytics.domain.pc.cf.dependency.validation.copulaValidator")
        ResourceBundleRegistry.addBundle(ResourceBundleRegistry.VALIDATION, "org.pillarone.riskanalytics.domain.pc.cf.dependency.validation.multipleProbabilitiesCopulaValidator")
        ResourceBundleRegistry.addBundle(ResourceBundleRegistry.VALIDATION, "org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.validation.xlStrategyValidator")
        ResourceBundleRegistry.addBundle(ResourceBundleRegistry.VALIDATION, "org.pillarone.riskanalytics.domain.pc.cf.structure.validation.claimTypeStructuringValidator")
        ResourceBundleRegistry.addBundle(ResourceBundleRegistry.VALIDATION, "org.pillarone.riskanalytics.domain.pc.cf.structure.validation.claimTypeStructuringValidator")
        ResourceBundleRegistry.addBundle(ResourceBundleRegistry.VALIDATION, "org.pillarone.riskanalytics.domain.pc.cf.reinsurance.cover.validation.coverAttributeValidator")
        ResourceBundleRegistry.addBundle(ResourceBundleRegistry.VALIDATION, "org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.proportional.lossparticipation.lossParticipationValidator")
        ResourceBundleRegistry.addBundle(ResourceBundleRegistry.VALIDATION, "org.pillarone.riskanalytics.domain.pc.cf.reinsurance.contract.proportional.commission.param.interpolatedSlidingCommissionValidator")

        // add resource bundle for exceptions
        ResourceBundleRegistry.addBundle(ResourceBundleRegistry.RESOURCE, "org.pillarone.riskanalytics.domain.pc.cf.exceptionResources")
        // doc urls
        ResourceBundleRegistry.addBundle(ResourceBundleRegistry.HELP, "org/pillarone/riskanalytics/domain/pc/cf/ComponentHelp")

        PacketAggregatorRegistry.registerAggregator(CCP, new ClaimPacketAggregator())
        PacketAggregatorRegistry.registerAggregator(UnderwritingInfoPacket, new UnderwritingInfoPacketAggregator())

        CollectingModeFactory.registerStrategy(new AggregateSplitPerSourceCollectingModeStrategy())
        CollectingModeFactory.registerStrategy(new AggregateSplitPerSourceReducedCollectingModeStrategy())

        CollectingModeFactory.registerStrategy(new AggregateUltimateClaimCollectingModeStrategy())
        CollectingModeFactory.registerStrategy(new AggregateUltimateReportedPaidClaimCollectingModeStrategy())
        CollectingModeFactory.registerStrategy(new AggregateUltimateReportedClaimCollectingModeStrategy())
        CollectingModeFactory.registerStrategy(new AggregateUltimatePaidClaimCollectingModeStrategy())

        CollectingModeFactory.registerStrategy(new SingleUltimatePaidClaimCollectingModeStrategy())

        // PMO-2231
        def claimFields = [CCP.PAID_INDEXED, CCP.PAID_CUMULATIVE_INDEXED, CCP.CHANGES_IN_RESERVES_INDEXED, CCP.RESERVES_INDEXED, CCP.CHANGES_IN_IBNR_INDEXED, CCP.IBNR_INDEXED,
                CCP.CHANGES_IN_OUTSTANDING_INDEXED, CCP.OUTSTANDING_INDEXED, CCP.REPORTED_INDEXED, CCP.REPORTED_CUMULATIVE_INDEXED, CCP.ULTIMATE, CCP.TOTAL_INCREMENTAL_INDEXED,
                CCP.TOTAL_CUMULATIVE_INDEXED, CCP.PREMIUM_RISK_BASE, CCP.RESERVE_RISK_BASE, CCP.PREMIUM_AND_RESERVE_RISK_BASE]

        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([], []))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_PERIOD], []))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_SOURCE], []))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_SOURCE, DrillDownMode.BY_PERIOD], []))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_SOURCE, DrillDownMode.BY_PERIOD], [0, 3, 5, 7, 8, 10, 11, 12, 13, 14, 15].collect { claimFields[it] },[CCP]))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_SOURCE, DrillDownMode.BY_PERIOD], [0, 3, 5, 7, 8, 10, 11, 12].collect { claimFields[it] },[CCP]))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_SOURCE, DrillDownMode.BY_PERIOD], [7, 11].collect { claimFields[it] },[CCP]))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_SOURCE], [7, 11].collect { claimFields[it] },[CCP]))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_PERIOD], [7, 11].collect { claimFields[it] },[CCP]))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([], [7, 11].collect { claimFields[it] },[CCP]))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_SOURCE, DrillDownMode.BY_PERIOD], [7, 11, 13, 14, 15].collect { claimFields[it] },[CCP]))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_SOURCE], [7, 11, 13, 14, 15].collect { claimFields[it] },[CCP]))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_PERIOD], [7, 11, 13, 14, 15].collect { claimFields[it] },[CCP]))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([], [7, 11, 13, 14, 15].collect { claimFields[it] },[CCP]))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([], [7, 10, 11, 12, 13, 14, 15].collect { claimFields[it] },[CCP]))

        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_PERIOD], [FP.GROSS_PREMIUM_RISK,FP.GROSS_RESERVE_RISK,FP.GROSS_PREMIUM_RESERVE_RISK], [FP],"FIN"))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([], [FP.GROSS_PREMIUM_RISK,FP.GROSS_RESERVE_RISK,FP.GROSS_PREMIUM_RESERVE_RISK], [FP],"FIN"))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_PERIOD], [], [FP,ContractFinancialsPacket],"FIN"))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([], [], [FP,ContractFinancialsPacket],"FIN"))

        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_PERIOD], [FP.GROSS_RESERVE_RISK, FP.GROSS_PREMIUM_RISK, FP.GROSS_PREMIUM_RESERVE_RISK, FP.NET_RESERVE_RISK, FP.NET_PREMIUM_RISK, FP.NET_PREMIUM_RESERVE_RISK, FP.CEDED_RESERVE_RISK, FP.CEDED_PREMIUM_RISK, FP.CEDED_PREMIUM_RESERVE_RISK], [FP],"GNC"))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_PERIOD], [FP.GROSS_RESERVE_RISK, FP.GROSS_PREMIUM_RISK, FP.GROSS_PREMIUM_RESERVE_RISK, FP.NET_RESERVE_RISK, FP.NET_PREMIUM_RISK, FP.NET_PREMIUM_RESERVE_RISK], [FP],"GN"))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([], [FP.GROSS_RESERVE_RISK, FP.GROSS_PREMIUM_RISK, FP.GROSS_PREMIUM_RESERVE_RISK, FP.NET_RESERVE_RISK, FP.NET_PREMIUM_RISK, FP.NET_PREMIUM_RESERVE_RISK], [FP],"GN"))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([], [FP.GROSS_RESERVE_RISK, FP.GROSS_PREMIUM_RISK, FP.GROSS_PREMIUM_RESERVE_RISK, FP.NET_RESERVE_RISK, FP.NET_PREMIUM_RISK, FP.NET_PREMIUM_RESERVE_RISK, FP.CEDED_RESERVE_RISK, FP.CEDED_PREMIUM_RISK, FP.CEDED_PREMIUM_RESERVE_RISK], [FP],"GNC"))

        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_PERIOD], [FP.GROSS_RESERVE_RISK, FP.GROSS_PREMIUM_RISK, FP.GROSS_PREMIUM_RESERVE_RISK, FP.NET_RESERVE_RISK, FP.NET_PREMIUM_RISK, FP.NET_PREMIUM_RESERVE_RISK, FP.CEDED_RESERVE_RISK, FP.CEDED_PREMIUM_RISK, FP.CEDED_PREMIUM_RESERVE_RISK, FP.GROSS_PREMIUM_WRITTEN, FP.NET_PREMIUM_WRITTEN, FP.CEDED_PREMIUM_WRITTEN], [FP],"GNC"))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_PERIOD], [FP.GROSS_RESERVE_RISK, FP.GROSS_PREMIUM_RISK, FP.GROSS_PREMIUM_RESERVE_RISK, FP.NET_RESERVE_RISK, FP.NET_PREMIUM_RISK, FP.NET_PREMIUM_RESERVE_RISK, FP.GROSS_PREMIUM_WRITTEN, FP.NET_PREMIUM_WRITTEN], [FP],"GN"))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([], [FP.GROSS_RESERVE_RISK, FP.GROSS_PREMIUM_RISK, FP.GROSS_PREMIUM_RESERVE_RISK, FP.NET_RESERVE_RISK, FP.NET_PREMIUM_RISK, FP.NET_PREMIUM_RESERVE_RISK, FP.GROSS_PREMIUM_WRITTEN, FP.NET_PREMIUM_WRITTEN], [FP],"GN"))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([], [FP.GROSS_RESERVE_RISK, FP.GROSS_PREMIUM_RISK, FP.GROSS_PREMIUM_RESERVE_RISK, FP.NET_RESERVE_RISK, FP.NET_PREMIUM_RISK, FP.NET_PREMIUM_RESERVE_RISK, FP.CEDED_RESERVE_RISK, FP.CEDED_PREMIUM_RISK, FP.CEDED_PREMIUM_RESERVE_RISK, FP.GROSS_PREMIUM_WRITTEN, FP.NET_PREMIUM_WRITTEN, FP.CEDED_PREMIUM_WRITTEN], [FP],"GNC"))


        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_PERIOD], [CCP.ULTIMATE, CCP.PAID_INDEXED]))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([], [CCP.ULTIMATE, CCP.PAID_INDEXED]))
        CollectingModeFactory.registerStrategy(new SplitAndFilterCollectionModeStrategy([DrillDownMode.BY_TYPE], [], [AdditionalPremium, PaidAdditionalPremium]))
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
