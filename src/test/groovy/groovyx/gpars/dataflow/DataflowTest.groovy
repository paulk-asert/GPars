// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.dataflow

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import static groovyx.gpars.actor.Actors.blockingActor

public class DataflowTest extends GroovyTestCase {

    public void testSimpleAssignment() {
        DataflowVariable<Integer> x = new DataflowVariable()
        DataflowVariable<Integer> y = new DataflowVariable()
        DataflowVariable<Integer> z = new DataflowVariable()

        def result = 0
        final def latch = new CountDownLatch(1)

        blockingActor {
            z << x.val + y.val
            result = z.val
            latch.countDown()
        }

        blockingActor {
            x << 40
        }
        blockingActor {
            y << 2
        }

        latch.await(90, TimeUnit.SECONDS)
        assertEquals 42, result
    }

    List<Integer> ints(int n, int max) {
        if (n == max) return []
        else return [n, * ints(n + 1, max)]
    }

    List<Integer> sum(int s, List<Integer> stream) {
        switch (stream.size()) {
            case 0: return [s]
            default:
                return [s, * sum(stream[0] + s, stream.size() > 1 ? stream[1..-1] : [])]
        }
    }

    public void testListAssignment() {
        def x = new DataflowVariable<List<Integer>>()
        def y = new DataflowVariable<List<Integer>>()

        def result = 0
        final def latch = new CountDownLatch(1)

        blockingActor { x << ints(0, 10) }
        blockingActor { y << sum(0, x.val) }
        blockingActor {
            result = y.val
            latch.countDown()
        }

        latch.await(90, TimeUnit.SECONDS)
        assertEquals([0, 0, 1, 3, 6, 10, 15, 21, 28, 36, 45], result)
    }

    void testRightShift() {
        DataflowVariable<Integer> x = new DataflowVariable()
        DataflowVariable<Integer> y = new DataflowVariable()
        DataflowVariable<Integer> z = new DataflowVariable()

        def result = 0
        final def latch = new CountDownLatch(1)

        z >> {res ->
            result = res
            latch.countDown()
        }

        blockingActor {
            z << x.val + y.val
        }

        blockingActor {x << 40}
        blockingActor {y << 2}

        latch.await(90, TimeUnit.SECONDS)
        assertEquals 42, result
    }

    void testMethodSyntax() {
        def df = new Dataflows()

        def result = 0
        final def latch = new CountDownLatch(1)

        df.z {res ->
            result = res
            latch.countDown()
        }

        blockingActor {
            def v = df.x + df.y
            df.z = v
        }

        blockingActor {
            df.x = 40
        }
        blockingActor {
            df.y = 2
        }

        latch.await(90, TimeUnit.SECONDS)
        assertEquals 42, result
    }
}
