import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IVehicle } from '../vehicle.model';
import { VehicleService } from '../service/vehicle.service';

@Component({
  templateUrl: './vehicle-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class VehicleDeleteDialogComponent {
  vehicle?: IVehicle;

  protected vehicleService = inject(VehicleService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.vehicleService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
