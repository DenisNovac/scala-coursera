/**
  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
  */
package actorbintree

import akka.actor._
import com.typesafe.scalalogging.LazyLogging

import scala.collection.immutable.Queue
import scala.language.postfixOps

object BinaryTreeSet {

  trait Operation {
    def requester: ActorRef
    def id: Int
    def elem: Int
  }

  trait OperationReply {
    def id: Int
  }

  /** Request with identifier `id` to insert an element `elem` into the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Insert(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to check whether an element `elem` is present
    * in the tree. The actor at reference `requester` should be notified when
    * this operation is completed.
    */
  case class Contains(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to remove the element `elem` from the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Remove(requester: ActorRef, id: Int, elem: Int) extends Operation

  /**
    * Эту штуку я добавил сам для чтения из актора его листа
    */
  case class Get(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request to perform garbage collection */
  case object GC

  /** Holds the answer to the Contains request with identifier `id`.
    * `result` is true if and only if the element is present in the tree.
    */
  case class ContainsResult(id: Int, result: Boolean) extends OperationReply

  /** Message to signal successful completion of an insert or remove operation. */
  case class OperationFinished(id: Int) extends OperationReply

}

class BinaryTreeSet extends Actor {
  import BinaryTreeSet._
  import BinaryTreeNode._

  def createRoot: ActorRef =
    context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))

  var root: ActorRef = createRoot

  // optional (used to stash incoming operations during garbage collection)
  var pendingQueue: Seq[Operation] = Queue.empty[Operation]

  // optional
  def receive = normal

  // optional
  /** Accepts `Operation` and `GC` messages. */
  val normal: Receive = {
    case Insert(req, id, elem) =>
      createRoot ! Insert(req, id, elem) // отослали рутовой ноде сообщение

    case Contains(req, id, elem) =>
      createRoot ! Contains(req, id, elem)

    case Remove(req, id, elem) =>
      createRoot ! Remove(req, id, elem)

    case Get(req, id, elem) =>
      createRoot ! Get(req, id, elem)

    case GC => ???

  }

  // optional
  /** Handles messages while garbage collection is performed.
    * `newRoot` is the root of the new binary tree where we want to copy
    * all non-removed elements into.
    */
  def garbageCollecting(newRoot: ActorRef): Receive = ???

}

object BinaryTreeNode {
  trait Position

  case object Left extends Position
  case object Right extends Position

  case class CopyTo(treeNode: ActorRef)

  /**
    * Acknowledges that a copy has been completed. This message should be sent
    * from a node to its parent, when this node and all its children nodes have
    * finished being copied.
    */
  case object CopyFinished

  def props(elem: Int, initiallyRemoved: Boolean): Props =
    Props(classOf[BinaryTreeNode], elem, initiallyRemoved)
}

class BinaryTreeNode(val elem: Int, initiallyRemoved: Boolean)
    extends Actor
    with LazyLogging {
  import BinaryTreeNode._
  import BinaryTreeSet._

  var subtrees: Map[Position, ActorRef] = Map[Position, ActorRef]()
  var removed: Boolean = initiallyRemoved

  def receive: Receive = normal

  /** Handles `Operation` messages and `CopyTo` requests. */
  val normal: Receive = {
    case Insert(req, id, e) =>
      e match {

        case e if e == elem => req ! OperationFinished(id)

        case e if e > elem =>
          if (subtrees.contains(Right))
            subtrees(Right) ! Insert(req, id, e)
          else {
            val actor = context.actorOf(props(e, initiallyRemoved = false))
            subtrees += (Right -> actor)
            req ! OperationFinished(id)
          }

        case e if e < elem =>
          if (subtrees.contains(Left))
            subtrees(Left) ! Insert(req, id, e)
          else {
            val actor = context.actorOf(props(e, initiallyRemoved = false))
            subtrees += (Left -> actor)
            req ! OperationFinished(id)
          }

      }

    case Contains(req, id, e) =>
      e match {
        case e if e == elem => req ! ContainsResult(id, result = true)
        case e if e < elem & subtrees.contains(Left) =>
          subtrees(Left) ! Contains(req, id, e)
        case e if e > elem & subtrees.contains(Right) =>
          subtrees(Right) ! Contains(req, id, e)
        case e => req ! ContainsResult(id, result = false)
      }

    case Remove(req, id, e) =>
      e match {
        case e if e == elem =>
          removed = true
          req ! OperationFinished(id)

        case e if e > elem & subtrees.nonEmpty =>
          subtrees(Right) ! Remove(req, id, e)
        case e if e < elem & subtrees.nonEmpty =>
          subtrees(Left) ! Remove(req, id, e)
        case e => req ! OperationFinished(id)
      }

    case Get(req, id, e) =>
      if (subtrees.contains(Right)) subtrees(Right) ! Get(req, id, e)
      if (subtrees.contains(Left)) subtrees(Left) ! Get(req, id, e)
      req ! subtrees
  }

  // optional
  /** `expected` is the set of ActorRefs whose replies we are waiting for,
    * `insertConfirmed` tracks whether the copy of this node to the new tree has been confirmed.
    */
  def copying(expected: Set[ActorRef], insertConfirmed: Boolean): Receive = ???

}
