package org.pillarone.riskanalytics.domain.pc.cf.output

import grails.test.GrailsUnitTestCase
import models.gira.GIRAModel
import org.joda.time.DateTime
import org.joda.time.Period
import org.pillarone.riskanalytics.core.output.CollectorMapping
import org.pillarone.riskanalytics.core.output.DrillDownMode
import org.pillarone.riskanalytics.core.output.FieldMapping
import org.pillarone.riskanalytics.core.output.PacketCollector
import org.pillarone.riskanalytics.core.output.PathMapping
import org.pillarone.riskanalytics.core.output.SingleValueResultPOJO
import org.pillarone.riskanalytics.core.packets.PacketList
import org.pillarone.riskanalytics.core.parameterization.StructureInformation
import org.pillarone.riskanalytics.core.simulation.LimitedContinuousPeriodCounter
import org.pillarone.riskanalytics.core.simulation.engine.IterationScope
import org.pillarone.riskanalytics.core.simulation.engine.MappingCache
import org.pillarone.riskanalytics.core.simulation.engine.PeriodScope
import org.pillarone.riskanalytics.core.simulation.engine.SimulationScope
import org.pillarone.riskanalytics.domain.pc.cf.claim.ClaimCashflowPacket
import org.pillarone.riskanalytics.domain.pc.cf.claim.generator.ClaimsGenerator
import org.pillarone.riskanalytics.domain.pc.cf.segment.Segment

/**
 * @author detlef.brendle (at) canoo (dot) com
 */
class SplitAndFilterCollectionModeStrategyTests extends GrailsUnitTestCase {
    SplitAndFilterCollectionModeStrategy strategy

    private void setupStrategy(def splitModes = [], def fieldFilter = [], def simulationStart = new DateTime(System.currentTimeMillis())) {
        mockDomain(PathMapping)
        mockDomain(CollectorMapping)
        mockDomain(FieldMapping)
        strategy = new SplitAndFilterCollectionModeStrategy(splitModes, fieldFilter)
        PacketCollector packetCollector = new PacketCollector(strategy)
        packetCollector.setPath("Path:to:collect")
        SimulationScope simulationScope = new SimulationScope()
        simulationScope.setStructureInformation(new StructureInformation(new ConfigObject(), new GIRAModel()))
        simulationScope.setMappingCache(new MappingCache())
        IterationScope iterationScope = new IterationScope()
        PeriodScope periodScope = new PeriodScope()
        periodScope.setPeriodCounter(new LimitedContinuousPeriodCounter(simulationStart, new Period(1, 0, 0, 0), 10))
        iterationScope.setPeriodScope(periodScope)
        simulationScope.setIterationScope(iterationScope)
        packetCollector.setSimulationScope(simulationScope)
        strategy.setPacketCollector(packetCollector)
    }

    void testCollectChanges_no_filter_no_split() throws IllegalAccessException {
        PacketList packets = new PacketList()
        def packet = new ClaimCashflowPacket()
        packets.add(packet)
        setupStrategy()
        List<SingleValueResultPOJO> result = strategy.collect(packets, false)
        assert packet.valuesToSave.size() == result.size()
    }

    void testCollectChanges_with_filter_no_split() throws IllegalAccessException {
        PacketList packets = new PacketList()
        def packet = new ClaimCashflowPacket()
        packets.add(packet)
        setupStrategy([], [ClaimCashflowPacket.ULTIMATE, ClaimCashflowPacket.CHANGES_IN_IBNR_INDEXED])
        List<SingleValueResultPOJO> result = strategy.collect(packets, false)
        assert 2 == result.size(), 'only the ultimate and changes in IBNR fields expected'
    }

    void testCollectChanges_no_filter_split_by_period() {
        def simulationStart = new DateTime(System.currentTimeMillis())
        PacketList packets = new PacketList()
        def packet = new ClaimCashflowPacket()
        packet.baseClaim.exposureStartDate = simulationStart
        packets.add(packet)
        setupStrategy([DrillDownMode.BY_PERIOD], [], simulationStart)
        List<SingleValueResultPOJO> result = strategy.collect(packets, false)
        assert 2 * packet.valuesToSave.size() == result.size()
    }

    void testCollectChanges_no_filter_split_by_source() {
        def simulationStart = new DateTime(System.currentTimeMillis())
        PacketList packets = new PacketList()
        def packet = new ClaimCashflowPacket()
        packets.add(packet)
        packet.senderChannelName = 'senderChannelName'
        packet.setSender(new Segment())
        packet.setMarker(new ClaimsGenerator(name: "testClaimsGenerator"))
        setupStrategy([DrillDownMode.BY_SOURCE], [], simulationStart)
        List<SingleValueResultPOJO> result = strategy.collect(packets, false)
        assert 2 * packet.valuesToSave.size() == result.size()
    }

    void testCollectChanges_no_filter_split_by_source_and_by_period() {
        def simulationStart = new DateTime(System.currentTimeMillis())
        PacketList packets = new PacketList()
        def packet = new ClaimCashflowPacket()
        packets.add(packet)
        packet.baseClaim.exposureStartDate = simulationStart
        packet.senderChannelName = 'senderChannelName'
        packet.setSender(new Segment())
        packet.setMarker(new ClaimsGenerator(name: "testClaimsGenerator"))
        setupStrategy([DrillDownMode.BY_SOURCE, DrillDownMode.BY_PERIOD], [], simulationStart)
        List<SingleValueResultPOJO> result = strategy.collect(packets, false)
        assert 3 * packet.valuesToSave.size() == result.size()
    }

    void testCollectChanges_with_filter_split_by_source_and_by_period() {
        def simulationStart = new DateTime(System.currentTimeMillis())
        PacketList packets = new PacketList()
        def packet = new ClaimCashflowPacket()
        packets.add(packet)
        packet.baseClaim.exposureStartDate = simulationStart
        packet.senderChannelName = 'senderChannelName'
        packet.setSender(new Segment())
        packet.setMarker(new ClaimsGenerator(name: "testClaimsGenerator"))
        setupStrategy([DrillDownMode.BY_SOURCE, DrillDownMode.BY_PERIOD], [ClaimCashflowPacket.CHANGES_IN_IBNR_INDEXED], simulationStart)
        List<SingleValueResultPOJO> result = strategy.collect(packets, false)
        assert 3 == result.size()
    }

    void testIdentifier(){
        strategy = new SplitAndFilterCollectionModeStrategy()
        assert 'AGGREGATE_NO-SPLIT_NO-FILTER' == strategy.identifier
        setupStrategy()
        assert 'AGGREGATE_NO-SPLIT_NO-FILTER' == strategy.identifier
        setupStrategy([DrillDownMode.BY_PERIOD])
        assert 'AGGREGATE_BY_PERIOD' == strategy.identifier
        setupStrategy([DrillDownMode.BY_SOURCE, DrillDownMode.BY_PERIOD])
        assert 'AGGREGATE_BY_SOURCE_BY_PERIOD' == strategy.identifier
        setupStrategy([DrillDownMode.BY_PERIOD],['field1'])
        assert 'AGGREGATE_BY_PERIOD_field1' == strategy.identifier
    }
}
