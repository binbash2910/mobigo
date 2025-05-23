import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../group-member.test-samples';

import { GroupMemberFormService } from './group-member-form.service';

describe('GroupMember Form Service', () => {
  let service: GroupMemberFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GroupMemberFormService);
  });

  describe('Service methods', () => {
    describe('createGroupMemberFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createGroupMemberFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            group: expect.any(Object),
            user: expect.any(Object),
          }),
        );
      });

      it('passing IGroupMember should create a new form with FormGroup', () => {
        const formGroup = service.createGroupMemberFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            group: expect.any(Object),
            user: expect.any(Object),
          }),
        );
      });
    });

    describe('getGroupMember', () => {
      it('should return NewGroupMember for default GroupMember initial value', () => {
        const formGroup = service.createGroupMemberFormGroup(sampleWithNewData);

        const groupMember = service.getGroupMember(formGroup) as any;

        expect(groupMember).toMatchObject(sampleWithNewData);
      });

      it('should return NewGroupMember for empty GroupMember initial value', () => {
        const formGroup = service.createGroupMemberFormGroup();

        const groupMember = service.getGroupMember(formGroup) as any;

        expect(groupMember).toMatchObject({});
      });

      it('should return IGroupMember', () => {
        const formGroup = service.createGroupMemberFormGroup(sampleWithRequiredData);

        const groupMember = service.getGroupMember(formGroup) as any;

        expect(groupMember).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IGroupMember should not enable id FormControl', () => {
        const formGroup = service.createGroupMemberFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewGroupMember should disable id FormControl', () => {
        const formGroup = service.createGroupMemberFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
