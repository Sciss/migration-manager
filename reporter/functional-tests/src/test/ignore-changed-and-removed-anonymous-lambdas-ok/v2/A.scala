class A {
  private var a = 5
  def foo() {
    val f1 = () => a
    val f2 = (x: Int) => (x * a).toString
  }
}
