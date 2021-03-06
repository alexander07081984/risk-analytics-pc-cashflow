package models.orsa

model = models.orsa.ORSAModel
displayName = "PMO-2347-RT"
components {
	reinsuranceContracts {
		subsubcomponents {
			outClaimsCeded = "AGGREGATE_BY_SOURCE_BY_PERIOD_outstandingIndexed_totalIncrementalIndexed_premiumRiskBase_reserveRiskBase_premiumAndReserveRiskBase"
			outClaimsGross = "AGGREGATE_BY_SOURCE_BY_PERIOD_outstandingIndexed_totalIncrementalIndexed_premiumRiskBase_reserveRiskBase_premiumAndReserveRiskBase"
			outClaimsNet = "AGGREGATE_BY_SOURCE_BY_PERIOD_outstandingIndexed_totalIncrementalIndexed_premiumRiskBase_reserveRiskBase_premiumAndReserveRiskBase"
		}
	}
	retrospectiveReinsurance {
		subsubcomponents {
			outClaimsCeded = "AGGREGATE_BY_SOURCE_BY_PERIOD_outstandingIndexed_totalIncrementalIndexed_premiumRiskBase_reserveRiskBase_premiumAndReserveRiskBase"
			outClaimsGross = "AGGREGATE_BY_SOURCE_BY_PERIOD_outstandingIndexed_totalIncrementalIndexed_premiumRiskBase_reserveRiskBase_premiumAndReserveRiskBase"
			outClaimsNet = "AGGREGATE_BY_SOURCE_BY_PERIOD_outstandingIndexed_totalIncrementalIndexed_premiumRiskBase_reserveRiskBase_premiumAndReserveRiskBase"
		}
	}
	segments {
		outClaimsCeded = "AGGREGATE_outstandingIndexed_ultimate_totalIncrementalIndexed_totalCumulativeIndexed_premiumRiskBase_reserveRiskBase_premiumAndReserveRiskBase"
		outClaimsGross = "AGGREGATE_outstandingIndexed_ultimate_totalIncrementalIndexed_totalCumulativeIndexed_premiumRiskBase_reserveRiskBase_premiumAndReserveRiskBase"
		outClaimsNet = "AGGREGATE_outstandingIndexed_ultimate_totalIncrementalIndexed_totalCumulativeIndexed_premiumRiskBase_reserveRiskBase_premiumAndReserveRiskBase"
		subsubcomponents {
			outClaimsCeded = "AGGREGATE_outstandingIndexed_ultimate_totalIncrementalIndexed_totalCumulativeIndexed_premiumRiskBase_reserveRiskBase_premiumAndReserveRiskBase"
			outClaimsGross = "AGGREGATE_outstandingIndexed_ultimate_totalIncrementalIndexed_totalCumulativeIndexed_premiumRiskBase_reserveRiskBase_premiumAndReserveRiskBase"
			outClaimsNet = "AGGREGATE_outstandingIndexed_ultimate_totalIncrementalIndexed_totalCumulativeIndexed_premiumRiskBase_reserveRiskBase_premiumAndReserveRiskBase"
			outFinancials = "AGGREGATE_FIN_BY_PERIOD"
		}
	}
	structures {
		substructure {
			outClaimsCeded = "AGGREGATE_outstandingIndexed_ultimate_totalIncrementalIndexed_totalCumulativeIndexed_premiumRiskBase_reserveRiskBase_premiumAndReserveRiskBase"
			outClaimsGross = "AGGREGATE_outstandingIndexed_ultimate_totalIncrementalIndexed_totalCumulativeIndexed_premiumRiskBase_reserveRiskBase_premiumAndReserveRiskBase"
			outClaimsNet = "AGGREGATE_outstandingIndexed_ultimate_totalIncrementalIndexed_totalCumulativeIndexed_premiumRiskBase_reserveRiskBase_premiumAndReserveRiskBase"
		}
	}
}
