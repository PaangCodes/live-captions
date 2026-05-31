import java.io.File

fun main() {
  val lines = File("app/src/main/java/dev/rongpi/livecaptions/download/ModelDownloader.kt").readLines()
  var braces = 0
  for ((i, line) in lines.withIndex()) {
    braces += line.count { it == '{' }
    braces -= line.count { it == '}' }
    if (braces < 0) {
      println("Unbalanced brace at line ${i+1}: $line")
      break
    }
  }
  println("Final brace count: $braces")
}
