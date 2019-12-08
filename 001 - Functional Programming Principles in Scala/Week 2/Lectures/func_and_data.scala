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


}

class Rational(x: Int, y: Int) {
  def numer = x
  def denom = y

  def add(that: Rational): Rational = 
    new Rational(
      numer * that.denom + that.numer * denom,
      denom * that.denom)

  def neg: Rational = new Rational(-numer, denom)

  // для вычитания прибавляем отрицательное число, меньше повтора кода
  def sub(that: Rational): Rational = add(that.neg)

  @Override
  override def toString(): String = numer.toString+"/"+denom.toString()
}