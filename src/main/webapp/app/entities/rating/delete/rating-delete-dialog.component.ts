import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IRating } from '../rating.model';
import { RatingService } from '../service/rating.service';

@Component({
  templateUrl: './rating-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class RatingDeleteDialogComponent {
  rating?: IRating;

  protected ratingService = inject(RatingService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.ratingService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
