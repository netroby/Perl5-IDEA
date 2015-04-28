// This is a generated file. Not intended for manual editing.
package com.perl5.lang.perl.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.perl5.lang.perl.lexer.PerlElementTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.perl5.lang.perl.psi.*;

public class PerlScalarValueDeterminedImpl extends ASTWrapperPsiElement implements PerlScalarValueDetermined {

  public PerlScalarValueDeterminedImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof PerlVisitor) ((PerlVisitor)visitor).visitScalarValueDetermined(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PerlMultilineMarker getMultilineMarker() {
    return findChildByClass(PerlMultilineMarker.class);
  }

  @Override
  @Nullable
  public PerlScalarAnonArray getScalarAnonArray() {
    return findChildByClass(PerlScalarAnonArray.class);
  }

  @Override
  @Nullable
  public PerlScalarAnonHash getScalarAnonHash() {
    return findChildByClass(PerlScalarAnonHash.class);
  }

  @Override
  @Nullable
  public PerlScalarValueMutable getScalarValueMutable() {
    return findChildByClass(PerlScalarValueMutable.class);
  }

  @Override
  @Nullable
  public PerlString getString() {
    return findChildByClass(PerlString.class);
  }

}
