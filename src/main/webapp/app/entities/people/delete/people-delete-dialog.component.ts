import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IPeople } from '../people.model';
import { PeopleService } from '../service/people.service';

@Component({
  templateUrl: './people-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class PeopleDeleteDialogComponent {
  people?: IPeople;

  protected peopleService = inject(PeopleService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.peopleService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
