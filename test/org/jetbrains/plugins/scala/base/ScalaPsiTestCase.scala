package org.jetbrains.plugins.scala.base

import _root_.com.intellij.testFramework.PsiTestCase
import _root_.org.jetbrains.plugins.scala.lang.psi.impl.toplevel.synthetic.SyntheticClasses
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.roots.{ModifiableRootModel, ContentEntry, OrderRootType, ModuleRootManager}
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.{VfsUtil, LocalFileSystem, VirtualFile}
import java.io.{File, IOException}
import lang.superMember.SuperMethodTestUtil
import util.TestUtils
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.projectRoots.JavaSdk
import _root_.org.scalatest.junit.JUnit3Suite

/**
 * User: Alexander Podkhalyuzin
 * Date: 24.02.2009
 */

abstract class ScalaPsiTestCase extends PsiTestCase with JUnit3Suite {
  private val JDK_HOME = TestUtils.getMockJdk

  protected val rootPath = TestUtils.getTestDataPath
  protected var testPath = ""

  protected var realOutput: String = ""

  override protected def setUp: Unit = {
    super.setUp
    myProject.getComponent(classOf[SyntheticClasses]).registerClasses
//    ScalaLoader.loadScala

    val rootModel = ModuleRootManager.getInstance(getModule).getModifiableModel

    val testDataRoot = LocalFileSystem.getInstance.findFileByPath(rootPath)
    val testName = getTestName(true) + ".scala"
    assert(testDataRoot != null)

    val contentEntry = rootModel.addContentEntry(testDataRoot);
    rootModel.setSdk(JavaSdk.getInstance.createJdk("java sdk", JDK_HOME, false))
    contentEntry.addSourceFolder(testDataRoot, false)

    // Add Scala Library
    val libraryTable = rootModel.getModuleLibraryTable
    val scalaLib = libraryTable.createLibrary("scala_lib")
    val libModel = scalaLib.getModifiableModel
    val libRoot = new File(TestUtils.getMockScalaLib)
    assert(libRoot.exists)

    val srcRoot = new File(TestUtils.getMockScalaSrc)
    assert(srcRoot.exists)

    libModel.addRoot(VfsUtil.getUrlForLibraryRoot(libRoot), OrderRootType.CLASSES)
    libModel.addRoot(VfsUtil.getUrlForLibraryRoot(srcRoot), OrderRootType.SOURCES)

    ApplicationManager.getApplication.runWriteAction(new Runnable {
      def run {
        libModel.commit
        rootModel.commit
      }
    })
  }

  /**
   * main tests runner. Please call it at the.
   */
  protected def doTest {
    val filePath = rootPath + testPath + ".scala"
    val vFile = LocalFileSystem.getInstance.findFileByPath(filePath.replace(File.separatorChar, '/'))
    assert(vFile != null, "file " + filePath + " not found")
    val output = getTestOutput(vFile)

    _root_.junit.framework.Assert.assertEquals(StringUtil.convertLineSeparators(realOutput), StringUtil.convertLineSeparators(output))
  }

  /**
   * Use this method to convert given file to output String.
   * This outout compared with <code>realOutput</code> string.
   * In the test method you should assign value to
   * <code>testPath</code> (relative to testdata)
   * and <code>realOutput</code>. For example:
   * <blockquote><pre>
   *      def testGenericClassApply{
   *        testPath = "/parameterInfo/apply/GenericClassApply"
   *        realOutput = "answer"
   *        playTest
   *      }
   * </pre></blockquote>
   * Be careful. This test class not suppert concurrent test
   * running, because of <code>realOutput</code> and
   * <code>testPath</code> fields.
   */
  protected def getTestOutput(file: VirtualFile): String = getTestOutput(file, true)
  protected def getTestOutput(file: VirtualFile, useOutput: Boolean): String

  /**
   * Tests generator.
   * @param testPath relative tests loaction (for example "paramterInfo")
   */
  protected def generateTests(testPath: String) {
    import com.intellij.openapi.fileTypes.FileTypeManager
    import com.intellij.openapi.vcs.VcsBundle
    import com.intellij.openapi.vfs.LocalFileSystem
    import com.intellij.openapi.vfs.VirtualFile
    import com.intellij.vcsUtil.VcsUtil
    import java.io.File
    import com.intellij.vcsUtil.VcsUtil
    import org.jetbrains.plugins.scala.util.TestUtils

    val testDataPath = TestUtils.getTestDataPath
    val testPaths = testDataPath + "/" + testPath

    val files = new java.util.ArrayList[VirtualFile]()
    VcsUtil.collectFiles(LocalFileSystem.getInstance.findFileByPath(testPaths.replace(File.separatorChar, '/')), files, true, false)
    for (file: VirtualFile <- files.toArray(Array[VirtualFile]()) if file.getExtension == "scala") {
      try {
        val method = getTestClass.getDeclaredMethod("test" + file.getNameWithoutExtension)
        method
      }
      catch {
        case e: NoSuchMethodException => {
          print("  def test" + file.getNameWithoutExtension + " {\n")
          val path = file.getPath
          print("    testPath = \"/" + path.substring(path.indexOf(testPath), path.indexOf(".scala")) + "\"\n")
          print("    realOutput = \"\"\"\n")
          print(getTestOutput(file, false) + "\n\"\"\"\n")
          print("    realOutput = realOutput.trim\n")
          print("    doTest\n  }\n\n")
        }
      }
    }
  }

  /**
   * override this to filter methods in tests generator
   */
  protected def getTestClass: Class[_] = classOf[ScalaPsiTestCase]
}
