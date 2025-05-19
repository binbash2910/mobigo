import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IStep } from '../step.model';
import { StepService } from '../service/step.service';

@Component({
  templateUrl: './step-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class StepDeleteDialogComponent {
  step?: IStep;

  protected stepService = inject(StepService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.stepService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
