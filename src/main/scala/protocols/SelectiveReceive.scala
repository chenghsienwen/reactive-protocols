package protocols

import akka.actor.typed.{ActorContext, _}
import akka.actor.typed.scaladsl._

object SelectiveReceive {
    /**
      * @return A behavior that stashes incoming messages unless they are handled
      *         by the underlying `initialBehavior`
      * @param bufferSize Maximum number of messages to stash before throwing a `StashOverflowException`
      *                   Note that 0 is a valid size and means no buffering at all (ie all messages should
      *                   always be handled by the underlying behavior)
      * @param initialBehavior Behavior to decorate
      * @tparam T Type of messages
      *
      * Hint: Implement an [[ExtensibleBehavior]], use a [[StashBuffer]] and [[Behavior]] helpers such as `start`,
      * `validateAsInitial`, `interpretMessage`,`canonicalize` and `isUnhandled`.
      */
    def apply[T](bufferSize: Int, initialBehavior: Behavior[T]): Behavior[T] = new ExtensibleBehavior[T] {
        private val buffer = StashBuffer[T](bufferSize)
        val started = Behavior.validateAsInitial(initialBehavior)
        override def receive(ctx: ActorContext[T], msg: T): Behavior[T] = {
            val next = Behavior.interpretMessage(started, ctx, msg)
            Behavior.isUnhandled(next) match {
                case true => {
                    buffer.stash(msg)
                    Behavior.same
                }
                case false => buffer.unstashAll(ctx.asScala, apply(bufferSize, Behavior.canonicalize(next, initialBehavior, ctx)))
            }
        }

        override def receiveSignal(ctx: ActorContext[T], msg: Signal): Behavior[T] = initialBehavior
    }
}
