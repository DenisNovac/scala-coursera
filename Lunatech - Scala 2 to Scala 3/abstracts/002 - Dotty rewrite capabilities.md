# Dotty's code rewrite capabilities

Дотти может переписывать код следующими способами:

- Заменять устаревший синтаксис;
- Заменять синтаксис на альтернативный.

Scala 3.0:

- Поддерживает синтаксис 2.13, в нём будут новые устаревшие места;
- Переписывает их опцией `-rewrite -source:3.0-migration` компилятора.

Scala 3.1:

- Поддерживает синтаксис Scala 3.0 с новыми устаревшими местами;
- Переписывает их опцией `-rewrite -soruce:3.1-migration`.

## Упражнение

Запустить sbt в корне проекта, выполнить `nextExercise` для перехода к первому упражнению и `man e` для инструкций:

```bash
man [e] > Scala 2 to Scala 3 > sudoku solver initial state > nextExercise
man [e] > Scala 2 to Scala 3 > dotty deprecated syntax rewriting > man e
```

Функции компилятора:

```
$ dotc -help
Usage: dotc <options> <source files>
where possible standard options include:
-P                 Pass an option to a plugin, e.g. -P:<plugin>:<opt>
-X                 Print a synopsis of advanced options.
-Y                 Print a synopsis of private options.

-rewrite           When used in conjunction with a ...-migration source version, rewrites sources to migrate to new version.

-source            source version
                   Default: 3.0.
                   Choices: 3.0, 3.1, 3.0-migration, 3.1-migration.
```

В проекте используются некоторый синтаксис Scala 2 для проверки работы. Выполнить из sbt:

```bash
pullTemplate scala/org/lunatechlabs/dotty/sudoku/SudokuSolver.scala
[INFO] Exercise scala/org/lunatechlabs/dotty/sudoku/SudokuSolver.scala pulled
```

Для удобства отслеживания изменений используется git. Текущее состояние фиксируется (изменения после pullTemplate):

```bash

git status

изменено:      exercises/.bookmark
изменено:      exercises/exercises/README.md
изменено:      exercises/exercises/src/main/scala/org/lunatechlabs/dotty/sudoku/SudokuSolver.scala
изменено:      exercises/exercises/src/test/resources/logback-test.xml

git add .
git commit -m "Snapshot before Dotty compiler rewrite"
```

Добавить в build.sbt в настройки упражнений опции компилятора:

```scala
lazy val `exercises` = project
  .configure(CommonSettings.configure)
  .dependsOn(common % "test->test;compile->compile")
  .settings(
    scalacOptions ++=
      Seq(
      "-source:3.0-migration"
      )
  )
```

Теперь показывает устаревший синтаксис:

```bash
man [e] > Scala 2 to Scala 3 > dotty deprecated syntax rewriting > reload
man [e] > Scala 2 to Scala 3 > dotty deprecated syntax rewriting > compile


[warn] -- Migration Warning: /home/denis/dev/home/scala-courses/Lunatech - Scala 2 to Scala 3/exercises/exercises/src/main/scala/org/lunatechlabs/dotty/sudoku/SudokuSolver.scala:172:35 
[warn] 172 |  private def checkHaha(s: String) {
[warn]     |                                   ^
[warn]     |Procedure syntax no longer supported; `: Unit =` should be inserted here
[warn] -- Migration Warning: /home/denis/dev/home/scala-courses/Lunatech - Scala 2 to Scala 3/exercises/exercises/src/main/scala/org/lunatechlabs/dotty/sudoku/SudokuSolver.scala:173:15 
[warn] 173 |    val haha = 'Haha
[warn]     |               ^
[warn]     | symbol literal 'Haha is no longer supported,
[warn]     | use a string literal "Haha" or an application Symbol("Haha") instead,
[warn]     | or enclose in braces '{Haha} if you want a quoted expression.
[warn] -- Migration Warning: /home/denis/dev/home/scala-courses/Lunatech - Scala 2 to Scala 3/exercises/exercises/src/main/scala/org/lunatechlabs/dotty/sudoku/SudokuSolver.scala:174:17 
[warn] 174 |    val noHaha = 'NoHaha
[warn]     |                 ^
[warn]     |symbol literal 'NoHaha is no longer supported,
[warn]     |use a string literal "NoHaha" or an application Symbol("NoHaha") instead,
[warn]     |or enclose in braces '{NoHaha} if you want a quoted expression.
[warn] three warnings found
```

Дописываем rewrite в настройки компиляции:

```scala
scalacOptions ++=
  Seq(
    "-source:3.0-migration",
     "-rewrite"
  )
  
```

И снова выполняем:

```scala
man [e] > Scala 2 to Scala 3 > dotty deprecated syntax rewriting > reload
man [e] > Scala 2 to Scala 3 > dotty deprecated syntax rewriting > compile

[info] [patched file /home/denis/dev/home/scala-courses/Lunatech - Scala 2 to Scala 3/exercises/exercises/src/main/scala/org/lunatechlabs/dotty/sudoku/SudokuSolver.scala]
[warn] three warnings found
```


Код изменился следующим образом:

```scala
private def checkHaha(s: String) {
  val haha = 'Haha
  val noHaha = 'NoHaha
  if (s startsWith haha.name) println(haha.name) else println(noHaha.name)
}


private def checkHaha(s: String): Unit = {
  val haha = Symbol("Haha")
  val noHaha = Symbol("NoHaha")
  if (s startsWith haha.name) println(haha.name) else println(noHaha.name)
}

```

Теперь сделаем то же самое с опцией `-source:3.1-migration`:

```scala
[warn] -- Deprecation Warning: /home/denis/dev/home/scala-courses/Lunatech - Scala 2 to Scala 3/exercises/exercises/src/main/scala/org/lunatechlabs/dotty/sudoku/SudokuSolver.scala:175:10 
[warn] 175 |    if (s startsWith haha.name) println(haha.name) else println(noHaha.name)
[warn]     |          ^^^^^^^^^^
[warn]     |Alphanumeric method startsWith is not declared @infix; it should not be used as infix operator.
[warn]     |The operation can be rewritten automatically to `startsWith` under -deprecation -rewrite.
[warn]     |Or rewrite to method syntax .startsWith(...) manually.
[info] [patched file /home/denis/dev/home/scala-courses/Lunatech - Scala 2 to Scala 3/exercises/exercises/src/main/scala/org/lunatechlabs/dotty/sudoku/SudokuSolver.scala]
```

И получаем:

```scala
private def checkHaha(s: String): Unit = {
  val haha = Symbol("Haha")
  val noHaha = Symbol("NoHaha")
  if (s `startsWith` haha.name) println(haha.name) else println(noHaha.name)
}
```

Разница видна в коммите #7624e55 (Snapshot after Dotty compiler rewrite).