object Classes extends App {
  //new MyClass(0) // выбросит эксепшен y must be positive
  //new MyClass(1,-1) // то же
  val mc = new MyClass(2)
  //mc.test(a) // метод приватен
  println(mc.nb1) // 3
  println(mc.nb2) // 1
  println(mc.nb3) // 4
  println(mc.method(1)) // 1
  println(mc.method(1,2)) // 3
  println(mc.toString) // (3, 1, 4)

  mc method 1
  mc.method(1)
}



class MyClass(x: Int, y: Int) {
  require(y > 0, "y must be positive")  // precondition, triggering an IllegalArgumentException if not met

  def this (x: Int) = { // вспомогательный (auxiliary) конструктор
    this(x+1, x-1)
  }

  def nb1 = x  // публичный метод вычисляется при каждом вызове
  def nb2 = y

  private def test(a: Int): Int = { // приватный метод
    if (a>0)
      a
    else
      0
  }

  def method(a: Int): Int = a
  // перегрузка метода
  def method(a: Int, b: Int): Int = a+b

  val nb3 = x + y // эта переменная вычисляется единожды

  // переопределение метода
  override def toString: String = {
    "("+x.toString+", "+y.toString+", "+nb3.toString+")"
  }
  //override def toString = member1 + ", " + member2 // overridden method
}

