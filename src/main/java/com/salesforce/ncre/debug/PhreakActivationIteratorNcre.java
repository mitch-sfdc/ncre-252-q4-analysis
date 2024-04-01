package com.salesforce.ncre.debug;

import java.util.*;

import com.salesforce.ncre.Engine;
import org.drools.base.reteoo.NodeTypeEnums;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.Memory;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.impl.InternalRuleBase;
import org.drools.core.reteoo.AccumulateNode;
import org.drools.core.reteoo.BetaMemory;
import org.drools.core.reteoo.BetaNode;
import org.drools.core.reteoo.FromNode;
import org.drools.core.reteoo.LeftInputAdapterNode;
import org.drools.core.reteoo.LeftTuple;
import org.drools.core.reteoo.LeftTupleSink;
import org.drools.core.reteoo.LeftTupleSource;
import org.drools.core.reteoo.ObjectSource;
import org.drools.core.reteoo.ObjectTypeNode;
import org.drools.core.reteoo.RightTuple;
import org.drools.core.reteoo.RuleTerminalNode;
import org.drools.core.reteoo.TerminalNode;
import org.drools.core.reteoo.Tuple;
import org.drools.core.reteoo.TupleMemory;
import org.drools.core.rule.consequence.InternalMatch;
import org.drools.core.util.FastIterator;
import org.drools.core.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhreakActivationIteratorNcre implements Iterator {
    private static final Logger LOG = LoggerFactory.getLogger(PhreakActivationIteratorNcre.class);
    private java.util.Iterator<InternalMatch> agendaItemIter;
    List<InternalMatch> internalMatches;
    private final StringBuilder graphBuilder = new StringBuilder("digraph{\n");
    private static final Map<Integer, String> nodeTypeToNodeNameMap = new HashMap<>();

    static {
        nodeTypeToNodeNameMap.put(10, "EntryPointNode");
        nodeTypeToNodeNameMap.put(20, "ReteNode");
        nodeTypeToNodeNameMap.put(30, "ObjectTypeNode");
        nodeTypeToNodeNameMap.put(40, "AlphaNode");
        nodeTypeToNodeNameMap.put(60, "WindowNode");
        nodeTypeToNodeNameMap.put(71, "RightInputAdapterNode");
        nodeTypeToNodeNameMap.put(80, "ObjectSource");
        nodeTypeToNodeNameMap.put(91, "QueryTerminalNode");
        nodeTypeToNodeNameMap.put(101, "RuleTerminalNode");
        nodeTypeToNodeNameMap.put(120, "EvalConditionNode");
        nodeTypeToNodeNameMap.put(131, "EvalConditionNode");
        nodeTypeToNodeNameMap.put(133, "TimerConditionNode");
        nodeTypeToNodeNameMap.put(135, "AsyncSendNode");
        nodeTypeToNodeNameMap.put(137, "AsyncReceiveNode");
        nodeTypeToNodeNameMap.put(141, "QueryRiaFixerNode");
        nodeTypeToNodeNameMap.put(151, "FromNode");
        nodeTypeToNodeNameMap.put(153, "ReactiveFromNode");
        nodeTypeToNodeNameMap.put(165, "UnificationNode/QueryElementNode");
        nodeTypeToNodeNameMap.put(171, "BetaNode");
        nodeTypeToNodeNameMap.put(181, "JoinNode");
        nodeTypeToNodeNameMap.put(191, "NotNode");
        nodeTypeToNodeNameMap.put(201, "ExistsNode");
        nodeTypeToNodeNameMap.put(211, "AccumulateNode");
        nodeTypeToNodeNameMap.put(221, "ForallNotNode");
        nodeTypeToNodeNameMap.put(231, "ElseNode");

    }

    private PhreakActivationIteratorNcre() {
    }

    private PhreakActivationIteratorNcre(ReteEvaluator reteEvaluator, InternalRuleBase kbase) {
        LOG.trace("Iterating KieSession");

        // add the main graph label
        graphBuilder.append("label=\"KieSession Visualizer\"\n")
                .append("tooltip=\"Visualizing session memory\"\n");

        this.internalMatches = collectAgendaItems(kbase, reteEvaluator, graphBuilder);
        this.agendaItemIter = this.internalMatches.iterator();

        // close the digraph representation
        graphBuilder.append("}\n");
    }

    public static PhreakActivationIteratorNcre iterator(ReteEvaluator reteEvaluator) {
        return new PhreakActivationIteratorNcre(reteEvaluator, reteEvaluator.getKnowledgeBase());
    }

    public Object next() {
        return this.agendaItemIter.hasNext() ? this.agendaItemIter.next() : null;
    }

    public static List<RuleTerminalNode> populateRuleTerminalNodes(InternalRuleBase kbase, Set<RuleTerminalNode> nodeSet) {
        LOG.trace("Enter populateRuleTerminalNodes()");

        Collection<TerminalNode[]> nodesWithArray = kbase.getReteooBuilder().getTerminalNodes().values();
        java.util.Iterator var3 = nodesWithArray.iterator();

        while(var3.hasNext()) {
            TerminalNode[] nodeArray = (TerminalNode[])var3.next();
            TerminalNode[] var5 = nodeArray;
            int var6 = nodeArray.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                TerminalNode node = var5[var7];
                if (node.getType() == 101) {
                    nodeSet.add((RuleTerminalNode)node);
                }
            }
        }

        return Arrays.asList((RuleTerminalNode[])nodeSet.toArray(new RuleTerminalNode[nodeSet.size()]));
    }

    public static List<InternalMatch> collectAgendaItems(InternalRuleBase kbase, ReteEvaluator reteEvaluator,
                                                         StringBuilder graphBuilder) {
        LOG.trace("Enter collectAgendaItems()");

        Set<RuleTerminalNode> nodeSet = new HashSet();
        List<RuleTerminalNode> nodeList = populateRuleTerminalNodes(kbase, nodeSet);
        List<InternalMatch> internalMatches = new ArrayList();
        java.util.Iterator var5 = nodeList.iterator();

        while(var5.hasNext()) {
            RuleTerminalNode rtn = (RuleTerminalNode)var5.next();
            graphBuilder.append("\"").append(rtn.getRule().getName()).append("\"")
                    .append(" [ shape=\"rect\" tooltip=\"")
                    .append(nodeTypeToNodeNameMap.get(Integer.valueOf(rtn.getType())))
                    .append("\" color=\"red\"]\n");
            if (nodeSet.contains(rtn)) {
                processLeftTuples(rtn.getLeftTupleSource(), internalMatches, nodeSet, reteEvaluator, graphBuilder);
            }
        }

        return internalMatches;
    }

    public static void processLeftTuples(LeftTupleSource node, List<InternalMatch> internalMatches,
                                         Set<RuleTerminalNode> nodeSet, ReteEvaluator reteEvaluator,
                                         StringBuilder graphBuilder) {
        LOG.trace("Enter processLeftTuples()");
        LeftTupleSource node1;

        graphBuilder.append("\"")
                .append(nodeTypeToNodeNameMap.get(Integer.valueOf(node.getType())))
                .append("-").append(node.getId()).append("\" []\n");

        // skip LeftInputAdapterNode (120) nodes
        for(node1 = node; 120 != node1.getType(); node1 = node1.getLeftTupleSource()) {
        }

        for(int maxShareCount = node1.getAssociationsSize(); 120 != node.getType(); node = node.getLeftTupleSource()) {
            Memory memory = reteEvaluator.getNodeMemories().peekNodeMemory(node);
            if (memory == null || memory.getSegmentMemory() == null) {
                return;
            }

            graphBuilder.append("\"")
                    .append(nodeTypeToNodeNameMap.get(Integer.valueOf(node1.getType())))
                    .append("-").append(node1.getId()).append("\" []\n");

            if (node.getAssociationsSize() == maxShareCount) {
                FastIterator it;
                LeftTuple lt;
                if (NodeTypeEnums.isBetaNode(node)) {
                    BetaMemory bm;
                    if (211 == node.getType()) {
                        AccumulateNode.AccumulateMemory am = (AccumulateNode.AccumulateMemory)memory;
                        bm = am.getBetaMemory();
                        it = bm.getLeftTupleMemory().fullFastIterator();

                        for(Tuple tuple = BetaNode.getFirstTuple(bm.getLeftTupleMemory(), it); tuple != null; tuple = (LeftTuple)it.next(tuple)) {
                            AccumulateNode.AccumulateContext accctx = (AccumulateNode.AccumulateContext)((Tuple)tuple).getContextObject();
                            LOG.trace("Calling collectFromPeers()");
                            collectFromPeers((LeftTuple)accctx.getResultLeftTuple(), internalMatches, nodeSet, reteEvaluator);
                        }
                    } else {
                        FastIterator fastIterator;
                        if (201 == node.getType()) {
                            bm = (BetaMemory)reteEvaluator.getNodeMemories().peekNodeMemory(node);
                            if (bm != null) {
                                bm = (BetaMemory)reteEvaluator.getNodeMemories().peekNodeMemory(node);
                                fastIterator = bm.getRightTupleMemory().fullFastIterator();

                                for(RightTuple rt = (RightTuple)BetaNode.getFirstTuple(bm.getRightTupleMemory(), fastIterator); rt != null; rt = (RightTuple)fastIterator.next(rt)) {
                                    for(lt = rt.getBlocked(); lt != null; lt = lt.getBlockedNext()) {
                                        if (lt.getFirstChild() != null) {
                                            LOG.trace("Calling collectFromPeers()");
                                            collectFromPeers(lt.getFirstChild(), internalMatches, nodeSet, reteEvaluator);
                                        }
                                    }
                                }
                            }
                        } else {
                            bm = (BetaMemory)reteEvaluator.getNodeMemories().peekNodeMemory(node);
                            if (bm != null) {
                                fastIterator = bm.getLeftTupleMemory().fullFastIterator();

                                for(Tuple tuple = BetaNode.getFirstTuple(bm.getLeftTupleMemory(), fastIterator); tuple != null; tuple = (LeftTuple)fastIterator.next(tuple)) {
                                    if (((Tuple)tuple).getFirstChild() != null) {
                                        collectFromLeftInput(((Tuple)tuple).getFirstChild(), internalMatches, nodeSet, reteEvaluator);
                                    }
                                }
                            }
                        }
                    }

                    return;
                }

                if (151 == node.getType()) {
                    FromNode.FromMemory fm = (FromNode.FromMemory)reteEvaluator.getNodeMemories().peekNodeMemory(node);
                    if (fm != null) {
                        TupleMemory ltm = fm.getBetaMemory().getLeftTupleMemory();
                        it = ltm.fullFastIterator();

                        for(lt = (LeftTuple)ltm.getFirst((Tuple)null); lt != null; lt = (LeftTuple)it.next(lt)) {
                            if (lt.getFirstChild() != null) {
                                collectFromLeftInput(lt.getFirstChild(), internalMatches, nodeSet, reteEvaluator);
                            }
                        }
                    }

                    return;
                }
            }
        }

        LeftInputAdapterNode lian = (LeftInputAdapterNode)node;
        if (!lian.isTerminal()) {
            Memory memory = reteEvaluator.getNodeMemories().peekNodeMemory(node);
            if (memory == null || memory.getSegmentMemory() == null) {
                return;
            }
        }

        ObjectSource os;
        for(os = lian.getObjectSource(); os.getType() != 30; os = os.getParentObjectSource()) {
        }

        ObjectTypeNode otn = (ObjectTypeNode)os;
        LeftTupleSink firstLiaSink = lian.getSinkPropagator().getFirstLeftTupleSink();
        java.util.Iterator<InternalFactHandle> it = otn.getFactHandlesIterator((InternalWorkingMemory)reteEvaluator);

        while(it.hasNext()) {
            InternalFactHandle fh = (InternalFactHandle)it.next();
            fh.forEachLeftTuple((ltx) -> {
                if (ltx.getTupleSink() == firstLiaSink) {
                    collectFromLeftInput(ltx, internalMatches, nodeSet, reteEvaluator);
                }

            });
        }

    }

    public String getGraph() {
        return graphBuilder.toString();
    }


    private static void collectFromLeftInput(LeftTuple lt, List<InternalMatch> internalMatches, Set<RuleTerminalNode> nodeSet, ReteEvaluator reteEvaluator) {
        LOG.trace("Enter collectFromLeftInput()");
        while(lt != null) {
            LOG.trace("Calling collectFromPeers()");
            collectFromPeers(lt, internalMatches, nodeSet, reteEvaluator);
            lt = lt.getHandleNext();
        }

    }

    private static void collectFromPeers(LeftTuple peer, List<InternalMatch> internalMatches, Set<RuleTerminalNode> nodeSet, ReteEvaluator reteEvaluator) {
        LOG.trace("Enter collectFromPeers()");

        for(; peer != null; peer = peer.getPeer()) {
            if (peer.getTupleSink().getType() == 211) {
                LOG.trace("Accumulate Peer Found");
                Object accctx = peer.getContextObject();
                if (accctx instanceof AccumulateNode.AccumulateContext) {
                    collectFromLeftInput((LeftTuple)((AccumulateNode.AccumulateContext)accctx).getResultLeftTuple(), internalMatches, nodeSet, reteEvaluator);
                }
            } else if (peer.getFirstChild() != null) {
                for(LeftTuple childLt = peer.getFirstChild(); childLt != null; childLt = childLt.getHandleNext()) {
                    collectFromLeftInput(childLt, internalMatches, nodeSet, reteEvaluator);
                }
            } else if (peer.getTupleSink().getType() == 101) {
                LOG.trace("RuleTerminalNode Peer Found");
                internalMatches.add((InternalMatch)peer);
                nodeSet.remove(peer.getTupleSink());
            }
        }

    }
}
