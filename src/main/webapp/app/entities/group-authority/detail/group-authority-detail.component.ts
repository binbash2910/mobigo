import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { IGroupAuthority } from '../group-authority.model';

@Component({
  selector: 'jhi-group-authority-detail',
  templateUrl: './group-authority-detail.component.html',
  imports: [SharedModule, RouterModule],
})
export class GroupAuthorityDetailComponent {
  groupAuthority = input<IGroupAuthority | null>(null);

  previousState(): void {
    window.history.back();
  }
}
