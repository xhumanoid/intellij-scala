class Input {
  def foo(x: Int) {
    val y = 34
    val z = ""
    /*start*/
    if (z == "") {
      val g = y + 1
      print(g)
    } else {
      print(x)
    }
    /*end*/
    if (x == 1) return
  }
}
/*
class Input {
  def testMethodName(x: Int, y: Int, z: String) {
    if (z == "") {
      val g = y + 1
      print(g)
    } else {
      print(x)
    }
  }

  def foo(x: Int) {
    val y = 34
    val z = ""
    /*start*/
    testMethodName(x, y, z)
    /*end*/
    if (x == 1) return
  }
}
*/