object test {
  val xs = Array(1, 2, 3, 4)
  xs map (_ * 2)

  val s = "Hello World"
  s filter (_.isUpper)
  s filter (c => c.isUpper)


  val r: Range = 1 until 5
  val r2: Range = 1 to 5
  val r3: Range = 1 to 5 by 2

  r3(0)
  r3(1)
  r3(2)
  //r3(4)

  r3 exists (_ == 5)
  r3 forall (_ > 0)
  r3 zip r
  r zip r3

  (r zip r3).unzip

  val r1: Range = 1 to 3
  r1.sum
  r1.product
  r1.max
  r1.min


  s flatMap (c => List('.', c))

  val M = 5
  val N = 10
  (1 until M) flatMap (x => (5 to N) map (y => (x, y)))


  /*def scalarProduct(xs: Vector[Double], ys: Vector[Double]): Double =
    (xs zip ys).map(xy => xy._1 * xy._2).sum*/

  def scalarProduct(xs: Vector[Double], ys: Vector[Double]): Double =
    (xs zip ys).map { case (x, y) => x * y }.sum


  val n = 4
  val del: Range = 2 until n
  !del.exists(i => (n > 0) & (n % i) == 0)

}



