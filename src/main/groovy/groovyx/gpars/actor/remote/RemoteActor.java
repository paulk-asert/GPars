package groovyx.gpars.actor.remote;

import groovyx.gpars.actor.Actor;
import groovyx.gpars.actor.ActorMessage;
import groovyx.gpars.actor.impl.MessageStream;
import groovyx.gpars.dataflow.expression.DataflowExpression;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.remote.RemoteHost;
import groovyx.gpars.serial.*;

import java.lang.reflect.InvocationTargetException;

public class RemoteActor extends Actor implements RemoteSerialized {
    private final RemoteHost remoteHost;
    private static final long serialVersionUID = -1375776678860848278L;

    public RemoteActor(final SerialContext host, final DataflowExpression<Object> jointLatch) {
        super(jointLatch);
        remoteHost = (RemoteHost) host;
    }

    @Override
    public Actor silentStart() {
        return null;
    }

    @Override
    public Actor start() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Actor stop() {
        remoteHost.write(new StopActorMsg(this));
        return this;
    }

    @Override
    public Actor terminate() {
        remoteHost.write(new TerminateActorMsg(this));
        return this;
    }

    @Override
    public boolean isActive() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean hasBeenStopped() {
        return false;  //todo implement
    }

    @Override
    protected ActorMessage sweepNextMessage() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings({"AssignmentToMethodParameter"})
    @Override
    public MessageStream send(Object message) {
        if (!(message instanceof ActorMessage)) {
            message = new ActorMessage(message, Actor.threadBoundActor());
        }
        remoteHost.write(new MessageStream.SendTo(this, (ActorMessage) message));
        return this;
    }

    public static class StopActorMsg extends SerialMsg {
        private final Actor actor;
        private static final long serialVersionUID = -927785591952534581L;

        public StopActorMsg(final RemoteActor remoteActor) {
            actor = remoteActor;
        }

        @Override
        public void execute(final RemoteConnection conn) {
            actor.stop();
        }
    }

    public static class TerminateActorMsg extends SerialMsg {
        private final Actor actor;
        private static final long serialVersionUID = -839334644951906974L;

        public TerminateActorMsg(final RemoteActor remoteActor) {
            actor = remoteActor;
        }

        @Override
        public void execute(final RemoteConnection conn) {
            actor.terminate();
        }
    }

    public static class MyRemoteHandle extends DefaultRemoteHandle {
        private final DataflowExpression<Object> joinLatch;
        private static final long serialVersionUID = 3721849638877039035L;

        public MyRemoteHandle(final SerialHandle handle, final SerialContext host, final DataflowExpression<Object> joinLatch) {
            super(handle.getSerialId(), host.getHostId(), RemoteActor.class);
            this.joinLatch = joinLatch;
        }

        @Override
        protected WithSerialId createObject(final SerialContext context) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
            return new RemoteActor(context, joinLatch);
        }
    }
}
