object Main extends App {
  val r = new Rational(1,2)
  // число - это хеш объекта
  println(r) // Rational@76ccd017
  // после объявления toString стало красиво
  println(r.numer) // 1

  val rr = new Rational(2,3)
  val n = r.add(rr)
  println(n) // 7/6

  val x = new Rational(1,3)
  val y = new Rational(5,7)
  val z = new Rational(3,2)

  println(x.sub(y).sub(z)) // -79/42

  println(y.add(y)) // 70/49  -- 10/7

  println(y.less(x))
  println(y.max(x))

  println(new Rational(42)) // 42/1

}

class Rational(x: Int, y: Int) {
  require(y != 0, "Denom must me not zero")

  def numer = x
  def denom = y

  // дополнительный конструктор вызывает главный конструктор
  def this(x: Int) = this(x, 1)


  def add(that: Rational): Rational = 
    new Rational(
      numer * that.denom + that.numer * denom,
      denom * that.denom)

  def neg: Rational = new Rational(-numer, denom)

  // для вычитания прибавляем отрицательное число, меньше повтора кода
  def sub(that: Rational): Rational = add(that.neg)

  def less(that: Rational) = numer * that.denom < that.numer * denom

  def max(that: Rational) = if (this.less(that)) that else this


  private def gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

  @Override
  override def toString(): String = {
    val g = gcd(x, y)
    (numer/g).toString+"/"+(denom/g).toString()
  }
}