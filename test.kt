import java.io.File
fun main() {
    val f = File("test.txt")
    f.writeText("hello")
    println(f.inputStream().buffered().javaClass.name)
}
