package csplugins.jActiveModulesHeadless.subnetSampling;

import java.util.Comparator;
import java.util.Iterator;
import java.util.AbstractQueue;
import java.util.NoSuchElementException;

public class BalancedOrderStatisticTree<T extends Comparable<T>> extends AbstractQueue<T> {
	
	
	protected Comparator<T> c;
	public BalancedOrderStatisticTree(){
		super();
		//creating an internal comparison function compare that I will use each time I want to use compareTo method. It enables me to have a second constructor where I can define in my own way what exactely I compare(used in class BalancedOrderStatisticTreeOrderedByPval)
		c=new Comparator<T>(){
			@Override
			public int compare(T o1, T o2) {
				return o1.compareTo(o2);
			}
		};
		
	}
	
	
	public BalancedOrderStatisticTree(Comparator<T> comparator){
		super();
		
		c=comparator;
		
	}
	
	
	/**
	 * TNodes of the tree.
	 */
	protected class TNode {
		/**
		 * The value at this TNode.
		 */
		protected T val;
		/**
		 * Get the value at this TNode.
		 * @return the value at this TNode.
		 */
		public T value() { return val; }
		/**
		 * The parent of this TNode. If this TNode it the root of a tree, null.
		 */
		public TNode parent = null;
		/**
		 * The left child of this TNode.
		 */
		public TNode lChild = null;
		/**
		 * The right child of this TNode.
		 */
		public TNode rChild = null;
		/**
		 * Number of children to the left. (Used for order statistic-purposes.)
		 */
		public int left = 0;
		/**
		 * Number of children to the left. (Used for order statistic-purposes.)
		 */
		public int right = 0;
		/**
		 * The height of this subtree.
		 */
		public int height = 1;
		/**
		 * Create a new TNode with the given value.
		 * @param item
		 *   the value at this TNode
		 */
		public TNode(T item) {
			val = item;
		}
		/**
		 * Find the left-most (smallest) TNode under this one.
		 * @return the left-most subTNode
		 */
		public TNode head() {
			TNode tmp = this;
			while (tmp.lChild != null) {
				tmp = tmp.lChild;
			}
			return tmp;
		}
		/**
		 * Find the right-most (largest) TNode under this one.
		 * @return the right-most subTNode
		 */
		public TNode tail() {
			TNode tmp = this;
			while (tmp.rChild != null) {
				tmp = tmp.rChild;
			}
			return tmp;
		}
		/**
		 * Get the tree depth for this TNode. The root TNode has depth zero, its
		 * children depth one, etc.
		 * @return the depth of this TNode
		 */
		public int depth() {
			int c = 0;
			TNode tmp = this;
			while (tmp.parent != null) {
				tmp = tmp.parent;
				c++;
			}
			return c;
		}
		private int heightLeft() {
			int h = 0;
			if (lChild != null) h = lChild.height;
			return h;
		}
		private int heightRight() {
			int h = 0;
			if (rChild != null) h = rChild.height;
			return h;
		}
		private void rotateLeftUp(int rightHeight) {
			assert lChild != null;

			TNode child = lChild;
			int hLeft = child.heightLeft(), hRight = child.heightRight();

			lChild = child.rChild;
			child.rChild = this;

			child.parent = this.parent;
			this.parent = child;
			if (lChild != null) lChild.parent = this;
			if (child.parent == null) {
				root = child;
			} else {
				if (child.parent.lChild == this) {
					child.parent.lChild = child;
				} else {
					assert child.parent.rChild == this;
					child.parent.rChild = child;
				}
			}

			left = child.right;
			child.right = left + 1 + right;

			if (hRight > rightHeight) {
				height = hRight + 1;
			} else {
				height = rightHeight + 1;
			}
			if (hLeft > height) {
				child.height = hLeft + 1;
			} else {
				child.height = height + 1;
			}
			assert child.verify();
		}
		private void rotateRightUp(int leftHeight) {
			assert rChild != null;

			TNode child = rChild;
			int hLeft = child.heightLeft(), hRight = child.heightRight();

			rChild = child.lChild;
			child.lChild = this;

			child.parent = this.parent;
			this.parent = child;
			if (rChild != null) rChild.parent = this;
			if (child.parent == null) {
				root = child;
			} else {
				if (child.parent.lChild == this) {
					child.parent.lChild = child;
				} else {
					assert child.parent.rChild == this;
					child.parent.rChild = child;
				}
			}

			right = child.left;
			child.left = left + 1 + right;

			if (hLeft > leftHeight) {
				height = hLeft + 1;
			} else {
				height = leftHeight + 1;
			}
			if (hRight > height) {
				child.height = hRight + 1;
			} else {
				child.height = height + 1;
			}
			assert child.verify();
		}
		private boolean updateHeight() {
			int hLeft = heightLeft(), hRight = heightRight();
			if (hLeft > hRight + 1) {
				assert lChild != null;
				int hLLeft = lChild.heightLeft(), hLRight = lChild.heightRight();
				if (hLLeft < hLRight) {
					lChild.rotateRightUp(hLLeft);
				}
				rotateLeftUp(hRight);
				parent.height = 0; // Force height of parent (who we just rotated up there) to update
				return true;
			} else if (hRight > hLeft + 1) {
				assert rChild != null;
				int hRLeft = rChild.heightLeft(), hRRight = rChild.heightRight();
				if (hRLeft > hRRight) {
					rChild.rotateLeftUp(hRRight);
				}
				rotateRightUp(hLeft);
				parent.height = 0; // Force height of parent (who we just rotated up there) to update
				return true;
			}

			if (hLeft > hRight) {
				if (height != hLeft + 1) {
					height = hLeft + 1;
					return true;
				} else {
					return false;
				}
			} else {
				if (height != hRight + 1) {
					height = hRight + 1;
					return true;
				} else {
					return false;
				}
			}
		}
		/**
		 * Get the position of this TNode in the larger tree. The left-most
		 * (smallest) TNode will have rank zero, the next will have rank one,
		 * etc.
		 * @return the position of this TNode
		 * @see #select(int)
		 */
		public int rank() {
			int c = left;
			TNode tmp = this;
			while (tmp.parent != null) {
				if (tmp.parent.lChild != tmp) {
					c += tmp.parent.left + 1;
				}
				tmp = tmp.parent;
			}
			return c;
		}
		/**
		 * Get the TNode at the given position within this TNode's tree.
		 * @param index
		 *   the position of the TNode we want
		 * @return the TNode at the given position
		 * @see #rank()
		 */
		public TNode select(int index) {
			if (index == left) {
				return this;
			} else if (index < left) {
				return lChild.select(index);
			} else {
				return rChild.select(index - left - 1);
			}
		}
		/**
		 * Insert a new TNode into the tree.
		 * @param n
		 *   the new TNode
		 * @param useRight
		 *   whether to use the right-most insertion point
		 */
		private void insert(TNode n, boolean useRight) {
			int cmp = c.compare(n.value(), value());
			if (cmp == 0) {
				if (useRight) {
					cmp = 1;
				} else {
					cmp = -1;
				}
			}

			if (cmp < 0) { // n.value < this.value
				left++;
				if (lChild == null) {
					n.parent = this;
					lChild = n;
					if (rChild == null) {
						height++;
						for (TNode p = parent; p != null; p = p.parent) {
							if (!p.updateHeight()) break;
						}
					}
				} else {
					lChild.insert(n, useRight);
				}
			} else { // n.value > this.value
				right++;
				if (rChild == null) {
					n.parent = this;
					rChild = n;
					if (lChild == null) {
						height++;
						for (TNode p = parent; p != null; p = p.parent) {
							if (!p.updateHeight()) break;
						}
					}
				} else {
					rChild.insert(n, useRight);
				}
			}
		}
		/**
		 * Insert a new TNode into the tree to the right of any equivalent TNodes.
		 * @param n
		 *   the new TNode
		 * @see insert(TNode,boolean)
		 */
		public void insertRight(TNode n) {
			insert(n, true);
		}
		/**
		 * Insert a new TNode into the tree to the left of any equivalent TNodes.
		 * @param n
		 *   the new TNode
		 * @see insert(TNode,boolean)
		 */
		public void insertLeft(TNode n) {
			insert(n, false);
		}
		/**
		 * Get the next TNode in the tree. This is the TNode whose rank is one
		 * greater than the rank of the current TNode.
		 * @return the next-largest TNode, or null if there are no larger TNodes
		 * @see #rank()
		 * @see #prev()
		 */
		public TNode next() {
			if (rChild != null) {
				// We have a right child; look under there
				return rChild.head();
			} else if (parent == null) {
				return null;
			} else if (parent.lChild == this) {
				 return parent;
			} else {
				TNode tmp = this;
				while (tmp.parent != null && tmp.parent.rChild == tmp) {
					tmp = tmp.parent;
				}
				return tmp.parent;
			}
		}
		/**
		 * Get the previous TNode in the tree. This is the TNode whose rank is
		 * one less than the rank of the current TNode.
		 * @return the next-smallest TNode, or null if there are no smaller
		 *   TNodes
		 * @see #rank()
		 * @see #next()
		 */
		public TNode prev() {
			if (lChild != null) {
				return lChild.tail();
			} else if (parent == null) {
				return null;
			} else if (parent.rChild == this) {
				 return parent;
			} else {
				TNode tmp = this;
				while (tmp.parent != null && tmp.parent.lChild == tmp) {
					tmp = tmp.parent;
				}
				return tmp.parent;
			}
		}
		/**
		 * Find the TNode under this one with the given value.
		 * @param val
		 *   the value we're searching for
		 * @param useRight
		 *   whether to use the right-most matching TNode, or the left-most
		 * @return the subTNode if one is found, or null
		 */
		public TNode find(T val, boolean useRight) {
			int cmp = c.compare(value(), val);
			if (cmp < 0 || (cmp == 0 && useRight)) { // too early; seach right side
				TNode res = null;
				if (rChild != null) res = rChild.find(val, useRight);
				if (res == null && cmp == 0) return this;
				return res;
			} else { // too late; seach left side
				TNode res = null;
				if (lChild != null) res = lChild.find(val, useRight);
				if (res == null && cmp == 0) return this;
				return res;
			}
		}
		public TNode findLeft(T val) {
			return find(val, false);
		}
		public TNode findRight(T val) {
			return find(val, true);
		}
		public boolean verify() {
			int hLeft  = 0, hRight = 0;
			if (parent != null)
				assert parent.lChild == this || parent.rChild == this : "Parent of " + value() + " only has children " + (parent.lChild == null ? "(null)" : parent.lChild.value()) + " and " + (parent.rChild == null ? "(null)" : parent.rChild.value()) + " (parent = " + parent.value() + ")";
			if (lChild != null) {
				assert lChild.parent == this : "Left child (" + lChild.value() + ") has parent " + lChild.parent.value() + ", not " + value();
				lChild.verify();
				assert left == lChild.left + 1 + lChild.right : "Left count at TNode " + value() + " should be " + (lChild.left + 1 + lChild.right) + ", not " + left;
				hLeft = lChild.height;
			} else {
				assert left == 0;
			}
			if (rChild != null) {
				assert rChild.parent == this : "Right child (" + rChild.value() + ") has parent " + rChild.parent.value() + ", not " + value();
				rChild.verify();
				assert right == rChild.left + 1 + rChild.right : "Right count at TNode " + value() + " should be " + (rChild.left + 1 + rChild.right) + ", not " + right;
				hRight = rChild.height;
			} else {
				assert right == 0;
			}
			if (hLeft > hRight) {
				assert height == hLeft + 1 : "Height at TNode " + value() + " should be " + (hLeft + 1) + ", not " + height;
			} else {
				assert height == hRight + 1 : "Height at TNode " + value() + " should be " + (hRight + 1) + ", not " + height;
			}
			return true; // "false" would be if one of the assertions actually failed, in which case an exception is raised
		}
	}
	public class TreeIterator implements Iterator<T> {
		private TNode curr, last;
		private TreeIterator() {
			last = null;
			curr = head;
		}
		public boolean hasNext() {
			return curr != null;
		}
		public T next() {
			last = curr;
			curr = curr.next();
			return last.value();
		}
		public void remove() {
			BalancedOrderStatisticTree.this.removeTNode(last);
		}
	}

	private TNode root = null, head = null, tail = null;
	public boolean offer(T item) {
		addRight(item);
		return true;
	}
	public void addLeft(T item) {
		insert(item, false);
	}
	public void addRight(T item) {
		insert(item, true);
	}
	private void insert(T item, boolean useRight) {
		TNode n = new TNode(item);
		if (root == null) {
			root = head = tail = n;
		} else {
			if (useRight) {
				root.insertRight(n);
				if (c.compare(head.value(),item)> 0) head = n;
				if (c.compare(tail.value(),item) <= 0) tail = n;
			} else {
				root.insertLeft(n);
				if (c.compare(head.value(),item) >= 0) head = n;
				if (c.compare(tail.value(),item) < 0) tail = n;
			}
		}
		assert root.verify();
	}
	public T peek() {
		if (head == null) return null;
		return head.value();
	}
	public T poll() {
		if (head == null) return null;
		TNode n = head;
		removeTNode(head);
		return n.value();
	}
	@SuppressWarnings("unchecked")
	public boolean remove(Object o) {
		if (o == null) throw new NullPointerException();
		java.lang.reflect.Type parentType = getClass().getGenericSuperclass();
		java.lang.reflect.Type[] typeArgs = ((java.lang.reflect.ParameterizedType) parentType).getActualTypeArguments();
		if (! ((Class<T>)typeArgs[0]).isAssignableFrom(o.getClass()) ) throw new ClassCastException();
		return remove((T)o);
	}
	public boolean remove(T item) {
		return removeRight(item);
	}
	private boolean remove(T item, boolean useRight) {
		if (item == null) throw new NullPointerException();
		if (root == null) return false;
		TNode n = root.find(item, useRight);
		if (n == null) return false;
		assert c.compare(n.value(), item) == 0;
		return removeTNode(n);
	}
	public boolean removeLeft(T item) {
		return remove(item, false);
	}
	public boolean removeRight(T item) {
		return remove(item, true);
	}
	private boolean removeTNode(TNode n) {
		// Can't remove nothing...
		if (n == null) return false;

		if (n == head) head = head.next();
		if (n == tail) tail = tail.prev();

		if (n.lChild == null) {
			// Decrement left/right counts
			TNode tmp = n;
			while (tmp.parent != null) {
				if (tmp.parent.lChild == tmp) {
					tmp.parent.left--;
				} else {
					assert tmp.parent.rChild == tmp;
					tmp.parent.right--;
				}
				tmp = tmp.parent;
			}

			// if we have a child, update its parentage
			if (n.rChild != null) {
				n.rChild.parent = n.parent;
			}

			// if we're the root TNode, update that
			if (n.parent == null) {
				root = n.rChild;
			} else {
				// update the parent's children
				if (n.parent.lChild == n) {
					n.parent.lChild = n.rChild;
				} else {
					assert n.parent.rChild == n;
					n.parent.rChild = n.rChild;
				}
			}
			for (TNode p = n.parent; p != null; p = p.parent) {
				if (!p.updateHeight()) break;
			}
		} else if (n.rChild == null) {
			TNode tmp = n;
			while (tmp.parent != null) {
				if (tmp.parent.lChild == tmp) {
					tmp.parent.left--;
				} else {
					assert tmp.parent.rChild == tmp;
					tmp.parent.right--;
				}
				tmp = tmp.parent;
			}

			n.lChild.parent = n.parent;

			if (n.parent == null) {
				root = n.lChild;
			} else {
				if (n.parent.lChild == n) {
					n.parent.lChild = n.lChild;
				} else {
					assert n.parent.rChild == n;
					n.parent.rChild = n.lChild;
				}
			}
			for (TNode p = n.parent; p != null; p = p.parent) {
				if (!p.updateHeight()) break;
			}
		} else {
			TNode p = n.prev();
			// Couple of assertions. If these fail, prev() is broken; there should be a TNode between p and n
			assert (p.parent == n && n.lChild == p) || p == p.parent.rChild;
			assert p.rChild == null;
			// First, disconnect p from the existing tree
			boolean wasLChild = false;
			if (p.parent == n) {
				wasLChild = true;
				n.lChild = p.lChild;
				if (p.lChild != null) {
					p.lChild.parent = n;
				}
			} else {
				p.parent.rChild = p.lChild;
				if (p.lChild != null) {
					p.lChild.parent = p.parent;
				}
			}
			for (TNode tmp = p; tmp.parent != null; tmp = tmp.parent) {
				if (((wasLChild) && (tmp == p)) || (tmp.parent.lChild == tmp)) {
					tmp.parent.left--;
				} else {
					assert ((!wasLChild) && (tmp == p)) || (tmp.parent.rChild == tmp);
					tmp.parent.right--;
				}
			}
			for (TNode tmp = p; tmp.parent != null; tmp = tmp.parent) {
				if (!tmp.parent.updateHeight()) break;
			}

			// Then, insert p where n was
			p.parent = n.parent;
			p.rChild = n.rChild;
			p.lChild = n.lChild;
			p.left = n.left;
			p.right = n.right;
			p.height = n.height;
			// And clean up what used to point to n
			if (p.rChild != null) {
				p.rChild.parent = p;
			}
			if (p.lChild != null) {
				p.lChild.parent = p;
			}
			if (n.parent == null) {
				root = p;
			} else {
				if (n.parent.lChild == n) {
					p.parent.lChild = p;
				} else {
					assert n.parent.rChild == n;
					p.parent.rChild = p;
				}
			}
		}
		assert root == null || root.verify();
		return true;
	}
	private int rank(T item, boolean useRight) {
		if (item == null) throw new NullPointerException();
		if (root == null) return -1;
		TNode n = root.find(item, useRight);
		if (n == null) return -1;
		assert c.compare(n.value(), item) == 0;
		return n.rank();
	}
	public int rankLeft(T item) {
		return rank(item, false);
	}
	public int rankRight(T item) {
		return rank(item, true);
	}
	public T select(int index) {
		assert root == null || root.verify();
		if (index < 0 || index > size()) throw new NoSuchElementException();
		TNode n = root.select(index);
		assert n != null;
		//if (n == null) throw new NoSuchElementException();
		return n.value();
	}
	public int size() {
		if (root == null) return 0;
		return root.left + 1 + root.right;
	}
	public int height() {
		if (root == null) return 0;
		return root.height;
	}
	public Iterator<T> iterator() {
		return new TreeIterator();
	}
}
