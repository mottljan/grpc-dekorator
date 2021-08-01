package processor

import java.io.OutputStream

internal operator fun OutputStream.plusAssign(str: String) {
    this.write(str.toByteArray())
}
