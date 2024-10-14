import {Component, Input, Output, EventEmitter, Inject} from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent, MatDialogRef,
  MatDialogTitle
} from '@angular/material/dialog';
import {MatButton} from '@angular/material/button';

@Component({
  selector: 'delete-material-modal',
  templateUrl: './delete.component.html',
  standalone: true,
  imports: [
    MatDialogContent,
    MatDialogActions,
    MatDialogTitle,
    MatButton,
    MatDialogClose
  ]
})
export class DeleteMaterialModalComponent {
  constructor(
    public dialogRef: MatDialogRef<DeleteMaterialModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { id: number; name: string }
  ) {}

  onConfirm(): void {
    this.dialogRef.close(true); // retorna true quando o usuário confirma
  }

  onCancel(): void {
    this.dialogRef.close(false); // retorna false quando o usuário cancela
  }
}
