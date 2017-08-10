package csplugins.jActiveModulesHeadless.subnetSampling;


import java.util.Comparator;

import csplugins.jActiveModulesHeadless.networkUtils.*;

//works: public class BalancedOrderStatisticTreeOrderedByNodePval<T extends Comparable<T>> extends BalancedOrderStatisticTree<T> {
	
public class BalancedOrderStatisticTreeOrderedByNodePval extends BalancedOrderStatisticTree <Node>  {

	protected static Comparator<Node> cPval=new Comparator<Node>(){
		@Override
		public int compare(Node n1, Node n2) {
			return Double.compare(n1.getPvalue(),n2.getPvalue());
		}
		};
		
		public BalancedOrderStatisticTreeOrderedByNodePval(){
			super( cPval);
			
		}
	//faut-il en plus reecrire toutes les methodes utilisant compareTo?


//	protected class TNode  extends BalancedOrderStatisticTree<Node>.TNode  {
//		
//	
//	public TNode(Node item) {
//		super(item);
//		val = item.getPvalue();
//	}
//	}
//ecrire comparateur, affichage...
}

//<Node> extends BalancedOrderStatisticTree
//<T extends Comparable<T>>